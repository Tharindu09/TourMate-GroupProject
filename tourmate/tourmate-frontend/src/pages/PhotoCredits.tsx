import photos from '../data/photos.json';
export function PhotoCredits() {
  return <section className="page"><p className="eyebrow">BEHIND THE VIEWS</p><h1>Photo credits</h1>
    <p>Photographs from Wikimedia Commons, shared under the licenses below. Images are resized and cropped to fit. Demo accommodation and restaurant listings show Ella scenery, rather than a specific venue.</p>
    <div className="credits-grid">{Object.entries(photos).map(([key, photo]) => <article key={key} id={key}>
      <img src={photo.src} alt={photo.alt} loading="lazy" width="320" height="200"/>
      <h3>{photo.alt}</h3><p>{photo.author}</p>
      <a className="text-link" href={photo.source} target="_blank" rel="noreferrer">Original photograph</a>{' · '}
      <a className="text-link" href={photo.licenseUrl || photo.source} target="_blank" rel="noreferrer">{photo.license}</a>
    </article>)}</div>
  </section>;
}
