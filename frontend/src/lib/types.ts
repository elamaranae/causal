export interface Price {
  priceCurrency: string;
  priceAmount: number;
}

export interface DefaultSku {
  id: number;
  attributes: Record<string, string>;
  variantAttributes: Record<string, string>;
  price: Price;
}

export interface ProductListing {
  id: number;
  name: string;
  primaryThumbnailUrl: string | null;
  categoryId: number;
  inStock: boolean;
  defaultSku: DefaultSku;
}

export interface MediaItem {
  url: string;
  type: string;
  primary: boolean;
  thumbnail: string;
}

export interface Sku {
  id: number;
  attributes: Record<string, string>;
  variantAttributes: Record<string, string>;
  media: { id: number; media: MediaItem[] } | null;
  price: Price;
  stockQuantity: number;
}

export interface ProductShow {
  id: number;
  name: string;
  description: string | null;
  primaryThumbnailUrl: string | null;
  categoryId: number;
  primaryVariantKey: string;
  attributes: Record<string, string>;
  defaultSkuId: number;
  skus: Sku[];
}

export interface ProductCategory {
  id: number;
  name: string;
  description: string | null;
  parentId: number | null;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface SkuProduct {
  id: number;
  name: string;
  description: string;
  categoryId: number;
}

export interface SkuDetail {
  id: number;
  attributes: Record<string, string>;
  variantAttributes: Record<string, string>;
  media: { id: number; media: MediaItem[] } | null;
  price: Price;
  stockQuantity: number;
  product: SkuProduct;
}

export interface ApiCartItem {
  id: number;
  skuId: number;
  quantity: number;
}

export interface ApiCart {
  id: number;
  userId: number;
  items: ApiCartItem[];
}

export interface CartItem {
  cartItemId: number;
  skuId: number;
  quantity: number;
  sku: SkuDetail;
}

export interface User {
  id: string;
  email: string;
}

export interface Profile {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  currency: string;
  defaultAddressId: number | null;
}

export interface Address {
  id: number;
  userId: number;
  label: string | null;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  country: string;
  pincode: string;
  phoneNumber: string | null;
}

export enum OrderStatus {
  PENDING = 'PENDING',
  PENDING_RESERVATION = 'PENDING_RESERVATION',
  RESERVED = 'RESERVED',
  PAYMENT_INITIATED = 'PAYMENT_INITIATED',
  PAYMENT_SUCCESS = 'PAYMENT_SUCCESS',
  PAYMENT_FAILED = 'PAYMENT_FAILED',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  RESERVATION_FAILED = 'RESERVATION_FAILED',
  RESERVATION_EXPIRED = 'RESERVATION_EXPIRED',
}

export enum DeliveryStatus {
  PENDING = 'PENDING',
}

export interface OrderAddress {
  id: number;
  label: string | null;
  line1: string;
  line2: string | null;
  city: string;
  state: string;
  country: string;
  pincode: string;
  phoneNumber: string | null;
}

export interface OrderItem {
  id: number;
  skuId: number;
  quantity: number;
  skuName: string;
  skuDescription: string;
  deliveryStatus: DeliveryStatus;
  price: Price;
}

export interface Order {
  id: number;
  status: OrderStatus;
  total: Price;
  shippingAddress: OrderAddress | null;
  billingAddress: OrderAddress | null;
  items: OrderItem[];
}

export interface OrderListResponse {
  orders: Order[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
