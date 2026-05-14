import type {
  ApiResponse,
  Category,
  Order,
  Product,
  User
} from "../types/shared";

export const mockUsersResponse: ApiResponse<User[]> = {
  data: [],
  message: "Users retrieved successfully",
  status: 200
};

export const mockProductsResponse: ApiResponse<Product[]> = {
  data: [],
  message: "Products retrieved successfully",
  status: 200
};

export const mockCategoriesResponse: ApiResponse<Category[]> = {
  data: [],
  message: "Categories retrieved successfully",
  status: 200
};

export const mockOrdersResponse: ApiResponse<Order[]> = {
  data: [],
  message: "Orders retrieved successfully",
  status: 200
};
