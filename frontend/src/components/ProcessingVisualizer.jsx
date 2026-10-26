import { useEffect, useState } from 'react'

const steps = [
  'Upload received',
  'OpenCV preprocessing',
  'Tesseract OCR',
  'Answer segmentation',
  'NLP + concept matching',
  'Score + confidence',
  'Teacher review'
]

export default function ProcessingVisualizer({ active = false, liveEvent = null }) {
  const [step, setStep] = useState(active ? 0 : steps.length - 1)

  useEffect(() => {
    if (liveEvent && typeof liveEvent.stepIndex === 'number') {
      setStep(Math.max(0, Math.min(liveEvent.stepIndex, steps.length - 1)))
      return
    }
    if (!active) {
      setStep(steps.length - 1)
      return
    }
    setStep(0)
    const t = setInterval(() => setStep((s) => Math.min(s + 1, steps.length - 1)), 700)
    return () => clearInterval(t)
  }, [active, liveEvent])

  const liveMessage = liveEvent?.message
  const livePercent = liveEvent?.progressPercent

  return (
    <div className="process-card">
      <div className="process-head">
        <div>
          <span className="eyebrow">LIVE PIPELINE</span>
          <h3>Evaluation engine</h3>
          {liveMessage && (
            <p className="process-live-msg" style={{ fontSize: '0.85rem', color: 'var(--muted, #8b949e)', marginTop: '4px' }}>
              {liveMessage} {livePercent != null ? `(${livePercent}%)` : ''}
            </p>
          )}
        </div>
        <span className={active ? 'live-dot' : 'done-dot'}>
          {active ? (liveEvent?.stage || 'PROCESSING') : 'READY'}
        </span>
      </div>
      <div className="pipeline">
        {steps.map((s, i) => (
          <div
            className={`pipeline-step ${i <= step ? 'done' : ''} ${i === step && active ? 'current' : ''}`}
            key={s}
          >
            <span>{i < step ? '✓' : i + 1}</span>
            <b>{s}</b>
          </div>
        ))}
      </div>
    </div>
  )
}