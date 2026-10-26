import React, { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client'
import {
  PaperclipIcon,
  SparklesIcon,
  CheckIcon,
  CloseIcon
} from '../components/Icons.jsx'

const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
const MAX_SIZE = 25 * 1024 * 1024 // 25MB

export default function Upload() {
  const navigate = useNavigate()
  const inputRef = useRef(null)

  const [exams, setExams] = useState([])
  const [examId, setExamId] = useState('')
  const [file, setFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [progress, setProgress] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [loadingExams, setLoadingExams] = useState(true)
  const [dragging, setDragging] = useState(false)

  useEffect(() => {
    api.get('/api/exams')
      .then((res) => {
        const list = res.data?.data || []
        setExams(list)
        if (list.length > 0) setExamId(list[0].id)
      })
      .catch((err) => setError(err.message || 'Unable to load exams.'))
      .finally(() => setLoadingExams(false))
  }, [])

  function handleFile(candidate) {
    setError('')
    if (!candidate) return
    if (!ACCEPTED.includes(candidate.type)) {
      return setError('Unsupported file format. Please upload PNG, JPG, WEBP, or PDF.')
    }
    if (candidate.size > MAX_SIZE) {
      return setError('File exceeds the 25MB limit.')
    }

    setFile(candidate)
    if (candidate.type.startsWith('image/')) {
      const url = URL.createObjectURL(candidate)
      setPreviewUrl(url)
    } else {
      setPreviewUrl('')
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!file || !examId) {
      return setError('Please select an active exam and choose a file.')
    }

    setBusy(true)
    setProgress(0)
    setError('')

    const formData = new FormData()
    formData.append('file', file)

    try {
      const res = await api.post(`/api/answer-sheets/upload?examId=${examId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (evt) => {
          if (evt.total) {
            setProgress(Math.round((evt.loaded * 100) / evt.total))
          }
        }
      })

      const createdId = res.data?.data?.id
      if (createdId) {
        navigate(`/answer-sheets/${createdId}`)
      } else {
        navigate('/')
      }
    } catch (err) {
      setError(err.message || 'Upload failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="upload-page" style={{ maxWidth: '1100px', margin: '0 auto' }}>
      <div style={{ marginBottom: '28px' }}>
        <span className="eyebrow">INTELLIGENT INGESTION SUITE</span>
        <h1 style={{ font: '800 36px Manrope', margin: '8px 0' }}>
          Upload Answer Sheet
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '15px', maxWidth: '680px' }}>
          Drop scanned student answer sheets or multi-page PDF documents. OpenCV will automatically deskew, calibrate contrast, and segment answers for evaluation.
        </p>
      </div>

      <div className="upload-grid" style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1.4fr) minmax(320px, 0.9fr)', gap: '24px' }}>
        {/* Left: Drag and drop radar zone */}
        <div
          className={`surface dropzone ${dragging ? 'is-dragging' : ''}`}
          style={{
            minHeight: '400px',
            border: '2px dashed var(--line)',
            borderRadius: '24px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            padding: '36px',
            position: 'relative',
            overflow: 'hidden',
            background: dragging ? 'rgba(229, 9, 20, 0.05)' : 'var(--surface)'
          }}
          onClick={() => inputRef.current?.click()}
          onDragOver={(e) => {
            e.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault()
            setDragging(false)
            handleFile(e.dataTransfer.files?.[0])
          }}
        >
          {previewUrl ? (
            <div style={{ position: 'relative', width: '100%', maxHeight: '340px', overflow: 'hidden', borderRadius: '16px', border: '1px solid var(--line)' }}>
              <img src={previewUrl} alt="Preview" style={{ width: '100%', height: 'auto', display: 'block' }} />
              <button
                style={{
                  position: 'absolute',
                  top: '12px',
                  right: '12px',
                  background: 'rgba(0,0,0,0.75)',
                  color: '#fff',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  display: 'grid',
                  placeItems: 'center'
                }}
                onClick={(e) => {
                  e.stopPropagation()
                  setFile(null)
                  setPreviewUrl('')
                }}
              >
                <CloseIcon size={16} />
              </button>
            </div>
          ) : (
            <>
              <div
                style={{
                  width: '72px',
                  height: '72px',
                  borderRadius: '20px',
                  background: 'linear-gradient(135deg, rgba(229,9,20,0.2), rgba(140,82,255,0.2))',
                  display: 'grid',
                  placeItems: 'center',
                  color: 'var(--netflix-red)',
                  marginBottom: '20px'
                }}
              >
                <PaperclipIcon size={32} />
              </div>
              <h3 style={{ margin: '0 0 8px', font: '700 20px Manrope' }}>
                {file ? file.name : 'Drag & drop student answer sheet here'}
              </h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '13px', textAlign: 'center', margin: '0 0 18px' }}>
                {file ? `Ready: ${(file.size / 1024).toFixed(1)} KB` : 'Supports high-res PNG, JPG, WEBP, or PDF up to 25MB'}
              </p>
              <button className="btn-netflix-ghost" style={{ pointerEvents: 'none' }}>
                <span>Browse Files</span>
              </button>
            </>
          )}

          <input
            ref={inputRef}
            type="file"
            accept={ACCEPTED.join(',')}
            style={{ display: 'none' }}
            onChange={(e) => handleFile(e.target.files?.[0])}
          />
        </div>

        {/* Right: Ingestion Config */}
        <div className="surface" style={{ borderRadius: '24px', padding: '30px' }}>
          <span className="eyebrow">EVALUATION METADATA</span>
          <h2 style={{ font: '800 22px Manrope', margin: '6px 0 20px' }}>
            Target Assessment
          </h2>

          {error && <div className="error-banner">{error}</div>}

          <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '20px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '8px' }}>
                Select Exam / Subject
              </label>
              <select
                value={examId}
                onChange={(e) => setExamId(e.target.value)}
                disabled={loadingExams || busy}
                style={{ width: '100%' }}
                required
              >
                {loadingExams ? (
                  <option>Loading active exams...</option>
                ) : exams.length === 0 ? (
                  <option value="">No exams available — please create an exam first</option>
                ) : (
                  exams.map((ex) => (
                    <option key={ex.id} value={ex.id}>
                      {ex.title} ({ex.subject})
                    </option>
                  ))
                )}
              </select>
            </div>

            <button
              type="submit"
              className="btn-netflix-red"
              disabled={busy || !file || !examId}
              style={{ width: '100%', justifyContent: 'center', marginTop: '12px' }}
            >
              <SparklesIcon size={18} />
              <span>{busy ? `Uploading ${progress}%...` : 'Begin Smart Ingestion →'}</span>
            </button>

            {busy && (
              <div style={{ width: '100%', height: '8px', background: 'rgba(255,255,255,0.08)', borderRadius: '999px', overflow: 'hidden' }}>
                <div
                  style={{
                    width: `${progress}%`,
                    height: '100%',
                    background: 'linear-gradient(90deg, var(--netflix-red), var(--accent))',
                    transition: 'width 0.2s ease'
                  }}
                />
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  )
}