$base = Join-Path $PSScriptRoot 'backend/src/main/java/io/supportops'
$null = New-Item -ItemType Directory -Force -Path "$base/domain","$base/repository"
$models = @{
  User = @('app_users','String userId','String email','String name','String passwordHash','String role','String iban');
  Transaction = @('transactions','String transactionId','String userId','String reconciliationId','String iban','BigDecimal amount','String currency','String transactionType','String status','Instant createdAt','Instant updatedAt');
  Payment = @('payments','String transactionId','BigDecimal amount','String channelType','String accountType','String status','Instant createdAt');
  AccountTransaction = @('account_transactions','String transactionId','BigDecimal amount','String status','Instant createdAt');
  Rebate = @('rebates','String originalTransactionId','String rebateTransactionId','BigDecimal amount','String status','String failureReason','Instant createdAt');
  ServiceLog = @('service_logs','String serviceName','String level','String message','String requestId','String correlationId','String transactionId','String requestBody','String responseBody','Integer httpStatus','Instant createdAt');
  KafkaEvent = @('kafka_events','String topic','String eventKey','String transactionId','String payload','String status','Instant createdAt');
  Incident = @('incidents','String incidentId','String title','String description','String transactionId','String status','String severity','String rootCause','Instant createdAt','Instant resolvedAt');
  IncidentAnalysis = @('incident_analyses','UUID incidentId','String transactionId','String rootCause','BigDecimal confidence','String evidence','String provider','Instant createdAt');
  AuditLog = @('audit_logs','String userId','String action','String resourceType','String resourceId','String metadata','Instant createdAt')
}
foreach ($name in $models.Keys) {
  $fields = $models[$name]
  $body = "package io.supportops.domain;`nimport jakarta.persistence.*;`nimport java.util.UUID;`nimport java.time.Instant;`nimport java.math.BigDecimal;`n@Entity`n@Table(name=`"$($fields[0])`")`npublic class $name {`n    @Id private UUID id;`n    public UUID getId() { return id; }`n"
  foreach ($field in $fields[1..($fields.Length-1)]) {
    $type,$prop = $field.Split(' ')
    $getter = $prop.Substring(0,1).ToUpperInvariant()+$prop.Substring(1)
    $column = [regex]::Replace($prop,'([a-z])([A-Z])','$1_$2').ToLowerInvariant()
    $body += "    @Column(name=`"$column`") private $type $prop;`n    public $type get$getter() { return $prop; }`n"
  }
  $body += "}`n"
  [IO.File]::WriteAllText("$base/domain/$name.java",$body)
}
