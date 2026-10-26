import React, { useEffect, useState } from 'react'
import api from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { EmptyState, ErrorState } from '../components/AsyncState.jsx'
import { SparklesIcon, CheckIcon } from '../components/Icons.jsx'

export default function StudentLearning() {
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/api/analytics/student/${user.userId}`)
      .then((res) => setData(res.data?.data))
      .catch((cause) => setError(cause.message || 'Unable to load your learning plan.'))
      .finally(() => setLoading(false))
  }, [user.userId])

  if (loading) {
    return (
      <div className="surface async-state" style={{ padding: '36px', textAlign: 'center' }}>
        <p style={{ color: 'var(--text-muted)' }}>Synthesizing personalized learning plan from OCR evaluation evidence...</p>
      </div>
    )
  }

  if (error) return <ErrorState title="Learning plan unavailable" message={error} />
  if (!data) {
    return (
      <EmptyState
        title="No learning data available"
        message="Submit and evaluate an answer sheet to activate your personalized learning signals."
      />
    )
  }

  return (
    <div className="learning-page">
      {/* Netflix Banner */}
      <div className="netflix-billboard" style={{ minHeight: '260px', padding: '36px', marginBottom: '32px' }}>
        <div className="billboard-content">
          <div className="billboard-badge">
            <SparklesIcon size={14} />
            <span>AI CURRICULUM COPILOT</span>
          </div>
          <h1 className="billboard-title" style={{ fontSize: '38px', margin: '6px 0 12px' }}>
            Adaptive Learning Paths
          </h1>
          <p className="billboard-desc" style={{ margin: 0 }}>
            Precision insights derived from missing keywords, rubric contradictions, and semantic gaps in your recent exam evaluations.
          </p>
        </div>
      </div>

      {data.recommendations?.length ? (
        <section style={{ marginBottom: '32px' }}>
          <div className="section-title" style={{ marginBottom: '18px' }}>
            <div>
              <span className="eyebrow">DIAGNOSTIC RECOMMENDATIONS</span>
              <h2>Prioritized Concept Review</h2>
            </div>
          </div>
          <div className="recommendation-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
            {data.recommendations.map((rec) => (
              <article className="surface" key={rec.concept} style={{ borderRadius: '20px', padding: '24px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                  <span className={`tag ${rec.priority === 'CRITICAL' ? 'danger' : 'success'}`}>
                    {rec.priority}
                  </span>
                  <strong style={{ font: '800 22px Manrope', color: '#fff' }}>{rec.masteryPercent}% Mastery</strong>
                </div>
                <h3 style={{ margin: '0 0 8px', font: '700 18px Manrope', color: '#fff' }}>{rec.concept}</h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '13px', lineHeight: '1.6', margin: '0 0 16px' }}>{rec.reason}</p>
                <div style={{ borderTop: '1px solid var(--line)', paddingTop: '14px' }}>
                  <span style={{ fontSize: '11px', fontWeight: 800, color: 'var(--netflix-red)' }}>RECOMMENDED ACTIONS</span>
                  <ol style={{ margin: '8px 0 0', paddingLeft: '18px', color: '#cbd5e1', fontSize: '12.5px', lineHeight: '1.7' }}>
                    {(rec.actions || []).map((act) => (
                      <li key={act}>{act}</li>
                    ))}
                  </ol>
                </div>
              </article>
            ))}
          </div>
        </section>
      ) : (
        <EmptyState title="No critical recommendations" message="Your evaluated concepts indicate strong mastery." />
      )}

      {/* Strong vs Weak Concepts Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        <div className="surface" style={{ borderRadius: '20px' }}>
          <span className="eyebrow" style={{ color: 'var(--success)' }}>MASTERED CONCEPTS</span>
          <h2 style={{ font: '800 20px Manrope', margin: '6px 0 16px' }}>Strong Foundations</h2>
          {data.strongConcepts?.length ? (
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {data.strongConcepts.map((c) => (
                <span className="tag success" key={c} style={{ fontSize: '12px', padding: '6px 12px' }}>
                  ✓ {c}
                </span>
              ))}
            </div>
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Strong concepts will be highlighted after more evaluations.</p>
          )}
        </div>

        <div className="surface" style={{ borderRadius: '20px' }}>
          <span className="eyebrow" style={{ color: 'var(--danger)' }}>AREAS FOR GROWTH</span>
          <h2 style={{ font: '800 20px Manrope', margin: '6px 0 16px' }}>Concepts to Revisit</h2>
          {data.topWeakConcepts?.length ? (
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {data.topWeakConcepts.map((c) => (
                <span className="tag danger" key={c} style={{ fontSize: '12px', padding: '6px 12px' }}>
                  ! {c}
                </span>
              ))}
            </div>
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '13px' }}>No repeated weak concepts detected across your submissions.</p>
          )}
        </div>
      </div>
    </div>
  )
}