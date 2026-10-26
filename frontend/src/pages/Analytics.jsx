import { useEffect, useState } from 'react'
import api from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import CanvasChart from '../components/CanvasChart.jsx'
import { EmptyState, ErrorState } from '../components/AsyncState.jsx'

function Metric({ label, value }) {
  return <div><span>{label}</span><b>{value}</b></div>
}

export default function Analytics() {
  const { user } = useAuth()
  const isStudent = user?.role === 'STUDENT'
  const [exams, setExams] = useState([])
  const [examId, setExamId] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [retryNonce, setRetryNonce] = useState(0)

  useEffect(() => {
    setLoading(true)
    setError('')
    setData(null)
    const request = isStudent
      ? api.get(`/api/analytics/student/${user.userId}`)
      : api.get('/api/exams').then((response) => {
          const permittedExams = response.data.data || []
          setExams(permittedExams)
          if (!examId && permittedExams[0]) setExamId(String(permittedExams[0].id))
          return null
        })
    request.then((response) => {
      if (response?.data?.data) setData(response.data.data)
    }).catch((cause) => setError(cause.message || 'Unable to connect to AES-QA server.')).finally(() => setLoading(false))
  }, [isStudent, user?.userId, retryNonce])

  useEffect(() => {
    if (isStudent || !examId) return
    setLoading(true)
    setError('')
    setData(null)
    api.get(`/api/analytics/class/${examId}`)
      .then((response) => setData(response.data.data))
      .catch((cause) => setError(cause.message || 'Unable to load analytics for this exam.'))
      .finally(() => setLoading(false))
  }, [examId, isStudent, retryNonce])

  const questionValues = isStudent
    ? (data?.progressOverTime || []).map((point) => Number(point.marksPercent) || 0)
    : (data?.questionWisePerformance || []).map((point) => {
        const max = Number(point.maxMarks)
        return max > 0 ? (Number(point.averageMarks) / max) * 100 : 0
      })
  const questionLabels = isStudent
    ? (data?.progressOverTime || []).map((point) => point.examTitle)
    : (data?.questionWisePerformance || []).map((point) => `Q${point.questionNumber}`)

  return <div>
    <div className="detail-hero">
      <div>
        <span className="eyebrow">INSIGHTS STUDIO</span>
        <h1 className="page-title">{isStudent ? 'My learning analytics' : 'Class analytics'}</h1>
        <p className="lead">{isStudent ? 'Track your own progress, subjects, and concepts that need another look.' : 'See where your cohort is strong, where concepts are missed, and how much review AI is requesting.'}</p>
      </div>
      {!isStudent && <select className="select-luxe" value={examId} onChange={(event) => setExamId(event.target.value)}><option value="">Choose exam</option>{exams.map((exam) => <option key={exam.id} value={exam.id}>{exam.title}</option>)}</select>}
    </div>
    {error && <ErrorState title="Analytics unavailable" message={error} onRetry={() => setRetryNonce((value) => value + 1)} />}
    {loading && <div className="analytics-grid analytics-loading"><div className="surface"><div className="skeleton-line wide"/><div className="skeleton-chart"/></div><div className="surface"><div className="skeleton-line wide"/><div className="skeleton-line"/><div className="skeleton-line"/><div className="skeleton-line"/></div></div>}
    {!loading && data && <>
      <div className="analytics-cards">
        {isStudent ? <><Metric label="ANSWER SHEETS" value={data.totalAnswerSheets}/><Metric label="AVERAGE" value={`${data.averageMarksPercent}%`}/><Metric label="HIGHEST" value={`${data.highestScorePercent}%`}/><Metric label="RECENT SCORE" value={`${data.recentScorePercent}%`}/><Metric label="IMPROVEMENT" value={`${data.improvementPercent}%`}/><Metric label="WEAK CONCEPTS" value={(data.topWeakConcepts || []).length}/></> : <><Metric label="MEAN" value={`${data.mean}%`}/><Metric label="MEDIAN" value={`${data.median}%`}/><Metric label="HIGHEST" value={`${data.highest}%`}/><Metric label="AI CONFIDENCE" value={`${Math.round((data.averageConfidence || 0) * 100)}%`}/></>}
      </div>
      <div className="analytics-grid">
        <div className="surface"><div className="section-title"><div><span className="eyebrow">PERFORMANCE</span><h2>{isStudent ? 'Progress over time' : 'Question-wise signal'}</h2></div></div>{questionValues.length ? <CanvasChart values={questionValues} labels={questionLabels} height={250}/> : <div className="empty-luxe">No evaluated data yet.</div>}</div>
        <div className="surface"><span className="eyebrow">{isStudent ? 'LEARNING SIGNALS' : 'REVIEW HEALTH'}</span><h2>{isStudent ? 'Concept mastery' : 'Operational metrics'}</h2>{isStudent ? <div className="metric-list">{Object.entries(data.conceptMasteryPercent || {}).map(([concept, mastery]) => <div key={concept}><span>{concept}</span><b>{mastery}%</b></div>)}{!Object.keys(data.conceptMasteryPercent || {}).length && <div className="empty-luxe">Complete an evaluation to build concept mastery.</div>}</div> : <div className="metric-list"><div><span>Answer sheets</span><b>{data.totalAnswerSheets}</b></div><div><span>Evaluated</span><b>{data.evaluatedCount}</b></div><div><span>Pending review</span><b>{data.pendingReviewCount}</b></div><div><span>Manual review rate</span><b>{data.manualReviewRatePercent}%</b></div></div>}</div>
      </div>
      {!isStudent && <section className="surface difficulty-list"><div className="section-title"><div><span className="eyebrow">QUESTION ANALYSIS</span><h2>Difficulty by question</h2></div></div>{(data.questionWisePerformance || []).map((question) => <div className="difficulty-row" key={question.questionId}><span>Q{question.questionNumber}</span><b>{question.difficultyClassification}</b><small>{question.attempts} attempt{question.attempts === 1 ? '' : 's'} · {question.difficultyClassification === 'INSUFFICIENT_DATA' ? 'More responses needed' : `${Math.round(question.difficultyEstimate * 100)}% difficulty`}</small></div>)}</section>}
    </>}
    {!loading && !error && !data && <EmptyState title="No analytics are available yet" message="Complete an evaluation to start building performance insight." />}
  </div>
}
