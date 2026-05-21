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
}

export interface ProductShow {
	id: number;
	name: string;
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

export interface CartItem {
	product: ProductListing;
	quantity: number;
}

export interface User {
	id: string;
	email: string;
}
