"""Live local-stack contract smoke test. Run after compose --profile simulation up."""
import json
import os
import time
import urllib.request
import urllib.error
import http.cookiejar

API = os.getenv("API_URL", "http://localhost:8080/api")
PASSWORD = os.getenv("DEMO_PASSWORD", "SupportOps-local-2026!")

def request(path, method="GET", data=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, data=None if data is None else json.dumps(data).encode(), headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=45) as response:
            status, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, raw = error.code, error.read()
    assert status == expected, (path, status, raw.decode()[:300])
    return json.loads(raw) if raw else None

def login(role):
    return request("/auth/login", "POST", {"email": role + "@supportops.local", "password": PASSWORD})["token"]

engineer, viewer, admin = [login(role) for role in ("engineer", "viewer", "admin")]
request("/dashboard", expected=401)
assert request("/dashboard", token=engineer)["stats"]["total_transactions"] >= 500
for identifier in ("abc-123", "usr-001", "rec-0001", "req-001", "corr-001", "TR720001002136978165000001"):
    assert request("/search?q=" + identifier, token=engineer)["transactions"]
for tx, expected in (("abc-123", "refundable balance"), ("tx-002", "Kafka"), ("tx-003", "timed out"), ("tx-004", "duplicate"), ("tx-005", "no corresponding account")):
    detail = request("/transactions/" + tx, token=viewer)
    stamps = [event["timestamp"] for event in detail["timeline"]]
    assert stamps == sorted(stamps), tx
    analysis = request("/transactions/" + tx + "/investigate", "POST", {}, engineer)
    assert expected.lower() in analysis["rootCause"].lower(), analysis
request("/transactions/abc-123/investigate", "POST", {}, viewer, 403)
request("/audit", token=viewer, expected=403)
preview = request("/ai-query/preview", "POST", {"prompt": "failed transactions"}, engineer)
assert "LIMIT 200" in preview["sql"]
rows = request("/ai-query/run", "POST", {"previewId": preview["id"]}, engineer)["rows"]
assert 0 < len(rows) <= 200
request("/ai-query/run", "POST", {"previewId": preview["id"]}, admin, 404)
incident = request("/incidents", "POST", {"title": "Smoke verification", "description": "Synthetic lifecycle verification", "transactionId": "abc-123", "severity": "LOW"}, engineer)
request("/incidents/" + incident["incidentId"], "PATCH", {"status": "RESOLVED", "rootCause": ""}, engineer, 400)
request("/incidents/" + incident["incidentId"], "PATCH", {"status": "RESOLVED", "rootCause": "Verified synthetic balance-zero scenario"}, engineer)
assert request("/incidents/" + incident["incidentId"], token=engineer)["incident"]["status"] == "RESOLVED"
assert request("/audit?q=SQL_QUERY_EXECUTED", token=admin)

# The browser-facing proxy must trust the configured public origin, never its
# internal Docker hostname. Authentication tokens remain HttpOnly cookies.
web = os.getenv("APP_ORIGIN", "http://localhost:3000")
jar = http.cookiejar.CookieJar()
browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
login_body = json.dumps({"email": "engineer@supportops.local", "password": PASSWORD}).encode()
for origin, expected in (("https://untrusted.invalid", 403), (web, 200)):
    req = urllib.request.Request(web + "/api/auth/login", data=login_body, headers={"Content-Type": "application/json", "Origin": origin})
    try:
        with browser.open(req, timeout=45) as response:
            assert response.status == expected
            cookie_header = response.headers.get("Set-Cookie", "")
            body = json.load(response)
            assert "token" not in body
            assert "HttpOnly" in cookie_header and "SameSite=strict" in cookie_header
    except urllib.error.HTTPError as error:
        assert error.code == expected, error.code
with browser.open(web + "/api/dashboard", timeout=30) as response:
    assert json.load(response)["stats"]["total_transactions"] >= 500
print("PASS browser proxy origin, HttpOnly session, authenticated dashboard")

for scenario in ("success", "rebate-zero", "timeout", "kafka-failed", "duplicate", "missing-account"):
    req = urllib.request.Request("http://localhost:8081/internal/simulate", data=json.dumps({"scenario": scenario}).encode(), headers={"Content-Type": "application/json", "X-Simulator-Token": os.getenv("SIMULATOR_TOKEN", "local-simulator-secret")})
    with urllib.request.urlopen(req, timeout=30) as response:
        tx = json.load(response)["transactionId"]
    deadline = time.monotonic() + 30
    while True:
        detail = request("/transactions/" + tx, token=engineer)
        if detail["kafka_events"]:
            break
        assert time.monotonic() < deadline, "Kafka evidence did not arrive: " + tx
        time.sleep(1)
    assert detail["transaction"]["status"] == ("FAILED" if scenario == "timeout" else "SUCCESS")
    if scenario in ("rebate-zero", "duplicate"):
        assert detail["rebates"][0]["status"] == "FAILED"
    print("PASS simulation", scenario, tx)
print("PASS auth, search, five investigations, RBAC, SQL preview/run/ownership, incident resolution, audit, six distributed scenarios")
