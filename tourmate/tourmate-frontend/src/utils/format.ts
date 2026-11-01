export const money = (value: number) => new Intl.NumberFormat('en-LK', {style: 'currency', currency: 'LKR', maximumFractionDigits: 0}).format(value);
const categories: Record<string, string> = {HOTEL: 'Stay', RESTAURANT: 'Eat & drink', ATTRACTION: 'See & explore', ACTIVITY: 'Get outside'};
export const category = (type: string) => categories[type] ?? type;
export const scenery = (id: number) => ['hill', 'lake', 'coast', 'rock', 'tea', 'beach', 'city'][(id - 1) % 7];
