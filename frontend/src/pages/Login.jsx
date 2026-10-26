import {useState} from 'react'
import {Link,useNavigate} from 'react-router-dom'
import {useAuth} from '../context/AuthContext.jsx'

export default function Login(){
  const {login}=useAuth(),nav=useNavigate()
  const [email,setEmail]=useState(''),[password,setPassword]=useState('')
  const [err,setErr]=useState(''),[busy,setBusy]=useState(false),[show,setShow]=useState(false)

  async function submit(e){
    e.preventDefault();setErr('');setBusy(true)
    try{await login(email,password);nav('/')}
    catch(x){setErr(x.message)}
    finally{setBusy(false)}
  }

  return <div className="auth-page">
    <aside className="auth-visual">
      <div className="brand large"><div className="brand-mark">A</div><div><strong>AES-QA</strong><small>Answer evaluation</small></div></div>
      <div className="auth-hero">
        <h1>Evaluate answer sheets <i>faster and fairer.</i></h1>
        <p>AI-assisted OCR and rubric-based grading, with teachers in control of every final mark.</p>
      </div>
      <small className="auth-copy">© {new Date().getFullYear()} AES-QA</small>
    </aside>

    <main className="auth-panel">
      <div className="auth-box">
        <h2>Sign in</h2>
        <p className="muted">Enter your credentials to access your account.</p>
        {err&&<div className="error-banner" role="alert">{err}</div>}
        <form onSubmit={submit}>
          <label>Email
            <input type="email" value={email} onChange={e=>setEmail(e.target.value)} required autoComplete="email" placeholder="you@college.edu"/>
          </label>
          <label>Password
            <span className="pw-field">
              <input type={show?'text':'password'} value={password} onChange={e=>setPassword(e.target.value)} required autoComplete="current-password" placeholder="Enter your password"/>
              <button type="button" className="pw-toggle" onClick={()=>setShow(s=>!s)} aria-label={show?'Hide password':'Show password'}>{show?'Hide':'Show'}</button>
            </span>
          </label>
          <button className="lux-btn full" disabled={busy}>{busy?'Signing in…':'Sign in'}</button>
        </form>
        <p className="auth-foot">Don't have an account? <Link to="/register">Create one</Link></p>
      </div>
    </main>
  </div>
}
