export interface ProductListing {
	id: number;
	name: string;
	primaryThumbnailUrl: string | null;
	categoryId: number;
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
