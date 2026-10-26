/**
 * Frontend-side contracts for the CampusHub API.
 * These are intentionally lightweight placeholders until backend DTOs are finalized.
 */
export const models = {
  User: ['id', 'name', 'avatar', 'school', 'token'],
  SecondhandItem: ['id', 'title', 'category', 'price', 'originalPrice', 'seller', 'location', 'condition', 'description', 'cover'],
  Activity: ['id', 'date', 'type', 'title', 'location', 'people', 'capacity', 'host', 'status'],
  Question: ['id', 'author', 'avatar', 'time', 'title', 'desc', 'answers', 'likes', 'tags'],
  CartLine: ['itemId', 'title', 'price', 'quantity', 'selected']
}

export function createCartLine(item, quantity = 1) {
  return { itemId: item.id, title: item.title, price: item.price, quantity, selected: true, seller: item.seller, cover: item.cover }
}
