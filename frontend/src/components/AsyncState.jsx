export function LoadingState({ label = 'Loading', className = '' }) {
  return <div className={`surface async-state ${className}`} role="status" aria-live="polite"><div className="skeleton-line wide"/><p className="muted">{label}</p></div>
}

export function ErrorState({ title = 'Something went wrong', message, onRetry }) {
  return <div className="surface async-state error-state" role="alert"><div className="error-banner"><strong>{title}.</strong>{message && ` ${message}`}</div>{onRetry && <button className="ghost-btn" onClick={onRetry}>Retry</button>}</div>
}

export function EmptyState({ title = 'Nothing here yet', message }) {
  return <div className="surface async-state empty-luxe"><strong>{title}</strong>{message && <p className="muted">{message}</p>}</div>
}
