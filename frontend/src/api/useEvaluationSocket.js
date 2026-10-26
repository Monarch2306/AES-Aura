import { useEffect, useRef, useState } from 'react'
import { getWebSocketUrl } from './client.js'

export function useEvaluationSocket(sheetId, onCompleted) {
  const [connected, setConnected] = useState(false)
  const [progressEvent, setProgressEvent] = useState(null)
  const socketRef = useRef(null)
  const onCompletedRef = useRef(onCompleted)

  useEffect(() => {
    onCompletedRef.current = onCompleted
  }, [onCompleted])

  useEffect(() => {
    if (!sheetId) return

    let isSubscribed = true
    const url = getWebSocketUrl(`/ws/evaluations/${sheetId}`)

    function connect() {
      try {
        const ws = new WebSocket(url)
        socketRef.current = ws

        ws.onopen = () => {
          if (!isSubscribed) return
          setConnected(true)
          try {
            ws.send(JSON.stringify({ action: 'subscribe', sheetId: Number(sheetId) }))
          } catch {
            // ignore send error
          }
        }

        ws.onmessage = (event) => {
          if (!isSubscribed) return
          try {
            const data = JSON.parse(event.data)
            if (data && (data.sheetId == null || String(data.sheetId) === String(sheetId))) {
              setProgressEvent(data)
              if (data.stage === 'COMPLETED' && onCompletedRef.current) {
                onCompletedRef.current(data)
              }
            }
          } catch {
            // ignore non-json frames
          }
        }

        ws.onclose = () => {
          if (!isSubscribed) return
          setConnected(false)
        }

        ws.onerror = () => {
          if (!isSubscribed) return
          setConnected(false)
        }
      } catch (err) {
        setConnected(false)
      }
    }

    connect()

    return () => {
      isSubscribed = false
      if (socketRef.current) {
        try {
          socketRef.current.close()
        } catch {
          // ignore close error
        }
        socketRef.current = null
      }
    }
  }, [sheetId])

  return { connected, progressEvent, resetProgress: () => setProgressEvent(null) }
}