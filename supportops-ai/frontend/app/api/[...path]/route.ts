import {NextRequest,NextResponse} from 'next/server';

async function proxy(request:NextRequest,context:{params:Promise<{path:string[]}>}) {
  const {path}=await context.params;
  const route=path.join('/');
  if(!/^(auth\/(login|logout)|dashboard|search|transactions(\/[^/]+(\/investigate)?)?|incidents(\/[^/]+)?|logs|audit|ai-query\/(preview|run))$/.test(route))return NextResponse.json({detail:'Not found'},{status:404});
  const appOrigin=process.env.APP_ORIGIN||'http://localhost:3000';
  if(request.method!=='GET' && request.headers.get('origin')!==appOrigin)return NextResponse.json({detail:'Invalid origin'},{status:403});
  if(route==='auth/logout') {const response=NextResponse.json({ok:true});response.cookies.delete('supportops');return response;}
  const token=request.cookies.get('supportops')?.value;
  if(route!=='auth/login'&&!token)return NextResponse.json({detail:'Sign in to continue'},{status:401});
  try {
    const upstream=await fetch(`${process.env.API_URL||'http://localhost:8080'}/api/${path.map(encodeURIComponent).join('/')}${request.nextUrl.search}`,{
      method:request.method,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},
      body:request.method==='GET'?undefined:await request.text(),cache:'no-store',signal:AbortSignal.timeout(40000)
    });
    const text=await upstream.text();
    const body=text?JSON.parse(text):{};
    if(route==='auth/login'&&upstream.ok){
      const response=NextResponse.json({name:body.name,role:body.role});
      response.cookies.set('supportops',body.token,{httpOnly:true,sameSite:'strict',secure:appOrigin.startsWith('https:'),path:'/',maxAge:3600});return response;
    }
    return NextResponse.json(body,{status:upstream.status===204?200:upstream.status});
  }catch{return NextResponse.json({detail:'Backend is unavailable. Check the API and Docker Compose services.'},{status:502});}
}
export {proxy as GET,proxy as POST,proxy as PATCH};
