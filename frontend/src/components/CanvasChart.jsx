import { useEffect, useRef } from 'react'
export default function CanvasChart({ values=[], labels=[], height=180 }){
  const ref=useRef(null)
  useEffect(()=>{
    const c=ref.current
    if (!c) return
    const ctx=c.getContext('2d'), dpr=window.devicePixelRatio||1
    const w=c.clientWidth||700; c.width=w*dpr; c.height=height*dpr; ctx.scale(dpr,dpr)
    ctx.clearRect(0,0,w,height)
    const cleanValues=(values||[]).map((v)=>Number.isFinite(Number(v)) ? Number(v) : 0)
    const max=Math.max(100,...cleanValues), pad={l:12,r:12,t:18,b:28}, cw=Math.max(10,w-pad.l-pad.r), ch=Math.max(10,height-pad.t-pad.b)
    ctx.font='12px Inter, sans-serif'; ctx.fillStyle='rgba(255,255,255,.45)';
    for(let i=0;i<=4;i++){ const y=pad.t+ch*i/4; ctx.strokeStyle='rgba(255,255,255,.08)';ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(w-pad.r,y);ctx.stroke();ctx.fillText(`${Math.round(max-(max*i/4))}`,2,y+4) }
    if(cleanValues.length<2) return
    const pts=cleanValues.map((v,i)=>({x:pad.l+(cw*i/(cleanValues.length-1)),y:pad.t+ch-(v/max)*ch}))
    const grad=ctx.createLinearGradient(0,pad.t,0,height);grad.addColorStop(0,'rgba(124,92,255,.30)');grad.addColorStop(1,'rgba(124,92,255,0)')
    ctx.beginPath();ctx.moveTo(pts[0].x,height-pad.b);pts.forEach(p=>ctx.lineTo(p.x,p.y));ctx.lineTo(pts.at(-1).x,height-pad.b);ctx.closePath();ctx.fillStyle=grad;ctx.fill()
    ctx.beginPath();pts.forEach((p,i)=>i?ctx.lineTo(p.x,p.y):ctx.moveTo(p.x,p.y));ctx.strokeStyle='#9d7cff';ctx.lineWidth=3;ctx.stroke()
    pts.forEach((p,i)=>{ctx.beginPath();ctx.arc(p.x,p.y,4,0,Math.PI*2);ctx.fillStyle='#fff';ctx.fill();ctx.beginPath();ctx.arc(p.x,p.y,2.5,0,Math.PI*2);ctx.fillStyle='#9d7cff';ctx.fill(); if(labels[i]){ctx.fillStyle='rgba(255,255,255,.5)';ctx.fillText(labels[i],p.x-14,height-8)}})
  },[values,labels,height])
  return <canvas ref={ref} className="canvas-chart" aria-label="Analytics chart" />
}
