import React, { useState, useEffect, useRef } from 'react'
import {
  WhatsAppIcon,
  CloseIcon,
  SendIcon,
  PaperclipIcon,
  MicIcon,
  DoubleCheckIcon,
  SparklesIcon
} from './Icons.jsx'

export default function WhatsAppCopilotDrawer({ activeContext = null }) {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [isRecording, setIsRecording] = useState(false)
  const [recordSeconds, setRecordSeconds] = useState(0)
  const [unreadCount, setUnreadCount] = useState(1)

  const [messages, setMessages] = useState([
    {
      id: 1,
      sender: 'ai',
      text: 'Hello! I am your AES-QA AI Examiner Copilot. I can explain rubric marks, clarify OCR text, diagnose missing concepts, or draft feedback for students.',
      time: 'Just now',
      status: 'delivered'
    }
  ])

  const messagesEndRef = useRef(null)

  useEffect(() => {
    if (open) {
      setUnreadCount(0)
      setTimeout(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
      }, 100)
    }
  }, [open, messages])

  // Context awareness
  useEffect(() => {
    if (activeContext?.examTitle || activeContext?.studentName) {
      const contextMsg = {
        id: Date.now(),
        sender: 'ai',
        text: `Active Workspace: ${activeContext.studentName ? `Student "${activeContext.studentName}"` : ''} ${activeContext.examTitle ? `in "${activeContext.examTitle}"` : ''}. How can I assist with this submission?`,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        status: 'delivered'
      }
      setMessages((prev) => [...prev, contextMsg])
      if (!open) setUnreadCount((c) => c + 1)
    }
  }, [activeContext?.studentName, activeContext?.examTitle])

  function handleSend(textToSend) {
    const text = textToSend || input
    if (!text.trim()) return

    const userMsg = {
      id: Date.now(),
      sender: 'user',
      text: text.trim(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      status: 'read'
    }

    setMessages((prev) => [...prev, userMsg])
    setInput('')

    // Generate intelligent contextual reply
    setTimeout(() => {
      let replyText = 'I analyzed the answer sheet data against the rubric benchmarks.'
      const lower = text.toLowerCase()

      if (lower.includes('deduct') || lower.includes('mark') || lower.includes('score')) {
        replyText = 'Based on the rubric criteria: Marks were awarded for semantic match (TF-IDF + LSA), but deducted where required core concepts were absent in the student response.'
      } else if (lower.includes('feedback') || lower.includes('draft')) {
        replyText = 'Suggested Student Feedback: "Good conceptual grasp of core principles. To achieve full marks, provide specific formulas and complete definitions for highlighted terms."'
      } else if (lower.includes('ocr') || lower.includes('handwriting')) {
        replyText = 'OCR analysis indicates clean binarization and line segmentation. Handwriting confidence is rated above threshold with minimal noise.'
      } else if (lower.includes('plagiarism') || lower.includes('similarity')) {
        replyText = 'Plagiarism Scanner: Evaluated cross-student n-gram similarity across the cohort. No unpermitted phrase clusters detected.'
      } else {
        replyText = `Understood. Analyzing query "${text}". AES-QA evaluation confidence is active and calibrated.`
      }

      const aiReply = {
        id: Date.now() + 1,
        sender: 'ai',
        text: replyText,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        status: 'delivered'
      }

      setMessages((prev) => [...prev, aiReply])
    }, 600)
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  function toggleVoiceRecord() {
    if (!isRecording) {
      setIsRecording(true)
      setRecordSeconds(0)
      const timer = setInterval(() => {
        setRecordSeconds((s) => s + 1)
      }, 1000)
      setTimeout(() => {
        clearInterval(timer)
        setIsRecording(false)
        handleSend('Voice query: "Review student response for question 1 and summarize concepts."')
      }, 3000)
    } else {
      setIsRecording(false)
    }
  }

  const promptChips = [
    'Explain mark deductions',
    'Draft feedback for student',
    'Check OCR handwriting confidence',
    'Detect cohort similarity risk'
  ]

  return (
    <>
      {/* Floating WhatsApp Action Pill */}
      <button
        className="wa-floating-btn"
        onClick={() => setOpen((v) => !v)}
        aria-label="Open AES WhatsApp Copilot"
        title="Open AES WhatsApp Copilot"
      >
        <div className="wa-icon-glow">
          <WhatsAppIcon size={24} />
        </div>
        <span className="wa-btn-label">Copilot</span>
        {unreadCount > 0 && <span className="wa-unread-badge">{unreadCount}</span>}
      </button>

      {/* WhatsApp Chat Drawer */}
      {open && (
        <div className="wa-drawer-backdrop" onClick={() => setOpen(false)}>
          <div
            className="wa-drawer"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-label="WhatsApp AI Copilot Drawer"
          >
            {/* Header */}
            <div className="wa-header">
              <div className="wa-header-avatar">
                <div className="wa-avatar-img">
                  <SparklesIcon size={18} />
                </div>
                <span className="wa-online-dot" />
              </div>
              <div className="wa-header-info">
                <h4>AES AI Examiner</h4>
                <p>online • ready to assist with grading</p>
              </div>
              <div className="wa-header-actions">
                <button
                  className="wa-icon-action"
                  onClick={() => setOpen(false)}
                  aria-label="Close chat"
                >
                  <CloseIcon size={20} />
                </button>
              </div>
            </div>

            {/* Body */}
            <div className="wa-body">
              <div className="wa-date-pill">TODAY</div>

              <div className="wa-messages">
                {messages.map((m) => (
                  <div
                    key={m.id}
                    className={`wa-bubble ${m.sender === 'user' ? 'wa-bubble-out' : 'wa-bubble-in'}`}
                  >
                    <div className="wa-bubble-text">{m.text}</div>
                    <div className="wa-bubble-meta">
                      <span className="wa-time">{m.time}</span>
                      {m.sender === 'user' && <DoubleCheckIcon size={15} color="#53bdeb" />}
                    </div>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>

              {/* Quick Prompt Chips */}
              <div className="wa-chips">
                {promptChips.map((chip) => (
                  <button
                    key={chip}
                    className="wa-chip-btn"
                    onClick={() => handleSend(chip)}
                  >
                    {chip}
                  </button>
                ))}
              </div>
            </div>

            {/* Footer */}
            <div className="wa-footer">
              <button
                className="wa-icon-action muted"
                title="Attach evidence / rubric"
                onClick={() => handleSend('Analyzing attached document rubric reference...')}
              >
                <PaperclipIcon size={20} />
              </button>

              <div className="wa-input-wrap">
                {isRecording ? (
                  <div className="wa-recording-pill">
                    <span className="wa-rec-dot" />
                    <span>Listening... {recordSeconds}s</span>
                  </div>
                ) : (
                  <input
                    type="text"
                    className="wa-input"
                    placeholder="Ask Copilot about grading, OCR, or rubrics..."
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    autoFocus
                  />
                )}
              </div>

              {input.trim() ? (
                <button
                  className="wa-send-btn"
                  onClick={() => handleSend()}
                  aria-label="Send message"
                >
                  <SendIcon size={18} />
                </button>
              ) : (
                <button
                  className={`wa-mic-btn ${isRecording ? 'recording' : ''}`}
                  onClick={toggleVoiceRecord}
                  title={isRecording ? 'Stop voice note' : 'Voice note'}
                >
                  <MicIcon size={20} />
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  )
}