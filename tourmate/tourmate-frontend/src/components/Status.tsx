export function Loading() {return <p className="status" role="status">Finding your next adventure…</p>;}
export function Failure({error, retry}: {error: Error; retry?: () => void}) {return <div className="status error" role="alert"><p>{error.message}</p>{retry && <button onClick={retry}>Try again</button>}</div>;}
