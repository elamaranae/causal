export interface Product {
	id: number;
	name: string;
	description: string;
	price: number;
}

export interface CartItem {
	product: Product;
	quantity: number;
}

export interface User {
	id: string;
	email: string;
}
