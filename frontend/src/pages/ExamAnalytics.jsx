import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api from '../api/client'
import CanvasChart from '../components/CanvasChart.jsx'
import { EmptyState, ErrorState } from '../components/AsyncState.jsx'

function Metric({ label, value }) {
  return <div><span>{label}</span><b>{value}</b></div>
}

export default function ExamAnalytics() {
  const { examId } = useParams()
  const [data, setData] = useState(null)
  const [exam, setExam] = useState(null)
  const [question, setQuestion] = useState('')
  const [delta, setDelta] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([api.get(`/api/analytics/exam/${examId}`), api.get(`/api/exams/${examId}`)])
      .then(([analytics, examResponse]) => { setData(analytics.data.data); setExam(examResponse.data.data) })
      .catch((cause) => setError(cause.message || 'Unable to load exam analytics.'))
      .finally(() => setLoading(false))
  }, [examId])

  if (loading) return <div className="surface async-state"><div className="skeleton-line wide"/><div className="skeleton-chart"/></div>
  if (error) return <ErrorState title="Exam analytics unavailable" message={error}/>
  if (!data) return <EmptyState title="No exam analytics" message="Real evaluated responses are required before analytics can be calculated."/>

  const questionsList = data.questions || []
  const selected = questionsList.find((item) => String(item.questionId) === question)
  const maxMarksNum = Number(selected?.maxMarks)
  const studentCountNum = Number(data.evaluatedStudentCount)
  const hypotheticalAverage = selected && studentCountNum > 0 && maxMarksNum > 0
    ? Number(data.averagePercent || 0) + (Number(delta) / maxMarksNum * 100) / studentCountNum
    : (data.averagePercent || 0)
  const questionValues = questionsList.map((item) => Number(item.averagePercent) || 0)
  const questionLabels = questionsList.map((item) => `Q${item.questionNumber}`)

  return <div>
    <div className="detail-hero"><div><span className="eyebrow">TEACHER ANALYTICS</span><h1 className="page-title">{exam?.title || 'Exam analytics'}</h1><p className="lead">Estimated difficulty and performance from persisted evaluations only.</p></div></div>
    <div className="analytics-cards"><Metric label="STUDENTS" value={data.studentCount}/><Metric label="AVERAGE" value={`${data.averagePercent ?? 0}%`}/><Metric label="MEDIAN" value={`${data.medianPercent ?? 0}%`}/><Metric label="RANGE" value={`${data.lowestPercent ?? 0}% - ${data.highestPercent ?? 0}%`}/></div>
    <div className="analytics-grid"><section className="surface"><div className="section-title"><div><span className="eyebrow">QUESTION PERFORMANCE</span><h2>Average percentage</h2></div></div>{questionsList.length ? <CanvasChart values={questionValues} labels={questionLabels} height={250}/> : <div className="empty-luxe">No evaluated questions yet.</div>}</section><section className="surface"><div className="section-title"><div><span className="eyebrow">SCORE DISTRIBUTION</span><h2>Students by score band</h2></div></div><div className="metric-list">{Object.entries(data.scoreDistribution || {}).map(([band, count]) => <div key={band}><span>{band}%</span><b>{count}</b></div>)}</div></section></div>
    <section className="surface difficulty-list"><div className="section-title"><div><span className="eyebrow">QUESTION DETAIL</span><h2>Question-wise analysis</h2></div></div><div className="table-wrap"><table><thead><tr><th>Question</th><th>Average</th><th>Difficulty</th><th>Concept coverage</th><th>Low / high</th></tr></thead><tbody>{questionsList.map((item) => <tr key={item.questionId}><td>Q{item.questionNumber}</td><td>{item.averagePercent}%</td><td>{item.difficulty}</td><td>{item.conceptCoveragePercent}%</td><td>{item.lowScorePercent}% / {item.highScorePercent}%</td></tr>)}</tbody></table></div></section>
    <div className="analytics-grid"><section className="surface"><span className="eyebrow">CONCEPT ANALYSIS</span><h2>Mastery signals</h2><div className="metric-list">{Object.entries(data.conceptMasteryPercent || {}).map(([concept, value]) => <div key={concept}><span>{concept}</span><b>{value}%</b></div>)}{!Object.keys(data.conceptMasteryPercent || {}).length && <div className="empty-luxe">No concept evidence is available.</div>}</div></section><section className="surface"><span className="eyebrow">TEACHER INSIGHTS</span><h2>Observed patterns</h2>{(data.insights || []).length ? <ul>{(data.insights || []).map((insight) => <li key={insight}>{insight}</li>)}</ul> : <div className="empty-luxe">No insight can be supported by the current data.</div>}</section></div>
    <section className="surface"><span className="eyebrow">HYPOTHETICAL SIMULATION</span><h2>What-if score simulator</h2><p className="muted">This does not modify persisted evaluations.</p><div className="form-grid"><label>Question<select className="select-luxe" value={question} onChange={(event) => setQuestion(event.target.value)}><option value="">Choose question</option>{questionsList.map((item) => <option key={item.questionId} value={item.questionId}>Q{item.questionNumber}</option>)}</select></label><label>Hypothetical mark change<input type="number" value={delta} onChange={(event) => setDelta(event.target.value)} step="0.5"/></label></div><div className="analytics-cards"><Metric label="CURRENT AVERAGE" value={`${data.averagePercent ?? 0}%`}/><Metric label="HYPOTHETICAL AVERAGE" value={`${Math.round(hypotheticalAverage * 100) / 100}%`}/></div></section>
  </div>
}
