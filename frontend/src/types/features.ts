export type SpringPage<T> = {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
};
export type RestockSubscription = {
  merchId: string;
  emailEnabled: boolean;
  subscribedAt: string;
};
export type Follow = {
  orgId: string;
  notifyMerch: boolean;
  notifyEvents: boolean;
  emailEnabled: boolean;
  followedAt: string;
};
export type PickupCredential = {
  orderId: string;
  token: string;
  pickupScheduleId: string | null;
  expiresAt: string;
};
export type OrderHistory = {
  id: string;
  orderId: string;
  actorId?: string;
  fromStatus: string;
  toStatus: string;
  source: string;
  pickupScheduleId?: string;
  createdAt: string;
};
export type CampaignState = "ACTIVE" | "SUCCEEDED" | "FAILED" | "CANCELLED";
export type CampaignVariant = {
  merchId: string;
  label: string;
  unitPrice: number;
  availableQuantity: number;
  available: boolean;
};
export type Campaign = {
  id: string;
  orgId: string;
  title: string;
  description?: string;
  minimumQuantity: number;
  deadline: string;
  state: CampaignState;
  reservedQuantity: number;
  createdAt: string;
  variants: CampaignVariant[];
};
export type Reservation = {
  id: string;
  campaignId: string;
  orderId: string;
  merchId: string;
  quantity: number;
  createdAt: string;
};
export type ReserveRequest = {
  merchId: string;
  quantity: number;
  requestId: string;
  note?: string;
};
export type CampaignOrderContext = {
  campaignId: string | null;
  campaignState: CampaignState | null;
  fulfillmentAllowed: boolean;
};
export type PurchaseContext = {
  campaignId: string | null;
  reservationRequired: boolean;
};
export type Analytics = {
  from: string;
  to: string;
  orders: {
    total: number;
    pending: number;
    confirmed: number;
    ready: number;
    completed: number;
    cancelled: number;
    completedQuantity: number;
    completedOrderValue: number;
    activeOrderValue: number;
    paidOrderValue: number;
    cancellationRate: number;
  };
  inventory: {
    products: number;
    publishedProducts: number;
    availableUnits: number;
    outOfStockProducts: number;
  };
  topProducts: {
    merchId: string;
    name: string;
    quantity: number;
    completedOrderValue: number;
  }[];
  dailyOrders: {
    date: string;
    orders: number;
    completed: number;
    cancelled: number;
    completedOrderValue: number;
  }[];
  pickupWorkload: {
    scheduleId: string;
    date: string;
    timeSlot: string;
    ready: number;
    completed: number;
  }[];
};
