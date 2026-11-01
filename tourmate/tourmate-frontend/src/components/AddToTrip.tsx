import {Link} from 'react-router-dom';
import type {SchedulePlace} from '../models/trip';
export function AddToTrip({place}: {place: SchedulePlace}) {
 return <Link className="add-to-trip text-link" to="/trips" state={{place}}>+ Add to trip</Link>;
}
