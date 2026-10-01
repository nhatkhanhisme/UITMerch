Backend fix impact checks — 1 October 2026

Upstream GitNexus analysis performed before source edits. Includes conservative checks of candidate methods and test methods; not every analyzed symbol will be modified. Graph omissions are not evidence of no callers. No commits or production database mutations.

| File / symbol | Direct callers | Process groups | Risk | Affected entries |
| --- | --- | --- | --- | --- |
| `wishlist/service/WishlistService.java:WishlistService` | 2 | 0 | LOW |  |
| `wishlist/service/WishlistService.java:getWishlist` | 2 | 2 | LOW | addItem, getWishlist |
| `wishlist/service/WishlistService.java:addItem` | 1 | 1 | LOW | addItem |
| `wishlist/service/WishlistService.java:removeItem` | 1 | 1 | LOW | removeItem |
| `wishlist/service/WishlistService.java:findOrCreate` | 3 | 3 | HIGH | getWishlist, removeItem, addItem |
| `order/controller/PublicOrderController.java:PublicOrderController` | 0 | 0 | UNKNOWN |  |
| `order/controller/PublicOrderController.java:guestCheckout` | 0 | 0 | UNKNOWN |  |
| `order/controller/PublicOrderController.java:trackGuestOrder` | 0 | 0 | UNKNOWN |  |
| `order/dto/GuestOrderRequest.java:GuestOrderRequest` | 4 | 0 | LOW |  |
| `order/dto/PickupScheduleRequest.java:PickupScheduleRequest` | 3 | 0 | LOW |  |
| `order/repository/OrderRepository.java:OrderRepository` | 3 | 0 | LOW |  |
| `order/service/OrderService.java:OrderService` | 6 | 0 | MEDIUM |  |
| `order/service/OrderService.java:createOrdersFromCart` | 1 | 1 | LOW | checkout |
| `order/service/OrderService.java:createInstantOrder` | 1 | 1 | LOW | createInstantOrder |
| `order/service/OrderService.java:createGuestOrder` | 1 | 1 | LOW | guestCheckout |
| `order/service/OrderService.java:getCustomerOrders` | 1 | 1 | LOW | getOrders |
| `order/service/OrderService.java:getCustomerOrder` | 1 | 1 | LOW | getOrder |
| `order/service/OrderService.java:getOrgOrders` | 1 | 1 | LOW | getOrgOrders |
| `order/service/OrderService.java:getOrgOrder` | 1 | 1 | LOW | getOrgOrder |
| `order/service/OrderService.java:updateOrderStatus` | 1 | 0 | LOW |  |
| `order/service/OrderService.java:checkInOrder` | 1 | 1 | LOW | checkIn |
| `order/service/OrderService.java:createPickupSchedule` | 1 | 1 | LOW | createPickupSchedule |
| `order/service/OrderService.java:getPickupSchedules` | 1 | 1 | LOW | getPickupSchedules |
| `order/service/OrderService.java:getPickupScheduleOrders` | 1 | 1 | LOW | getPickupScheduleOrders |
| `order/service/OrderService.java:cancelCustomerOrder` | 1 | 1 | LOW | cancelOrder |
| `order/service/OrderService.java:cancelOrgOrder` | 1 | 1 | LOW | cancelOrder |
| `order/service/OrderService.java:getGuestOrderByEmail` | 1 | 1 | LOW | trackGuestOrder |
| `order/service/OrderService.java:getAllOrders` | 1 | 1 | LOW | listAllOrders |
| `order/service/OrderService.java:validateStatusTransition` | 1 | 0 | LOW |  |
| `order/service/OrderService.java:applyCancel` | 2 | 2 | LOW | cancelOrder, cancelOrder |
| `order/service/OrderService.java:restoreStockForItems` | 2 | 2 | LOW | cancelOrder, cancelOrder |
| `order/service/OrderService.java:loadPickupSchedule` | 7 | 6 | CRITICAL | getOrders, getOrder, getOrgOrders, checkIn, getOrgOrder, trackGuestOrder |
| `order/service/OrderService.java:notifyOrganizer` | 7 | 6 | CRITICAL | checkout, guestCheckout, cancelOrder, createInstantOrder, cancelOrder, checkIn |
| `order/service/OrderService.java:notifyCustomerOrderPlaced` | 2 | 2 | LOW | checkout, createInstantOrder |
| `order/service/OrderService.java:notifyCustomerInApp` | 3 | 2 | LOW | cancelOrder, checkIn |
| `order/service/OrderService.java:sendCancelEmail` | 2 | 2 | LOW | cancelOrder, cancelOrder |
| `order/service/OrderService.java:notifyOrgOwnerOfCancel` | 1 | 1 | LOW | cancelOrder |
| `order/service/OrderService.java:sendPickupNotification` | 1 | 1 | LOW | createPickupSchedule |
| `order/service/OrderService.java:resolveCustomerEmail` | 2 | 5 | CRITICAL | cancelOrder, cancelOrder, sendPickupNotification, createPickupSchedule, sendCancelEmail |
| `merch/repository/MerchItemRepository.java:MerchItemRepository` | 10 | 0 | MEDIUM |  |
| `merch/service/MerchService.java:MerchService` | 5 | 0 | MEDIUM |  |
| `merch/service/MerchService.java:createMerch` | 1 | 1 | LOW | createMerch |
| `merch/service/MerchService.java:getOwnMerch` | 1 | 1 | LOW | getOwnMerch |
| `merch/service/MerchService.java:getOwnMerchItem` | 1 | 1 | LOW | getOwnMerchItem |
| `merch/service/MerchService.java:updateMerch` | 1 | 1 | LOW | updateMerch |
| `merch/service/MerchService.java:deleteMerch` | 1 | 1 | LOW | deleteMerch |
| `merch/service/MerchService.java:listPublished` | 2 | 2 | LOW | visualSearch, listMerch |
| `merch/service/MerchService.java:getPublishedMerch` | 1 | 1 | LOW | getMerch |
| `merch/service/MerchService.java:getPopularMerch` | 1 | 1 | LOW | getPopular |
| `merch/service/MerchService.java:listByOrganization` | 1 | 1 | LOW | getOrgMerch |
| `merch/service/MerchService.java:getPublishedMerchByIds` | 1 | 2 | LOW | visualSearch, vectorSearch |
| `merch/service/MerchService.java:getMerchEntityForOrder` | 0 | 0 | UNKNOWN |  |
| `merch/service/MerchService.java:saveImages` | 2 | 4 | HIGH | createMerch, updateMerch, createMerch, updateMerch |
| `merch/service/MerchService.java:loadImages` | 3 | 4 | HIGH | updateMerch, getOwnMerchItem, getMerch, updateMerch |
| `merch/service/MerchService.java:buildImageMap` | 5 | 6 | CRITICAL | visualSearch, getPopular, listMerch, getOwnMerch, getOrgMerch, vectorSearch |
| `merch/service/MerchService.java:findOwnItemOrThrow` | 3 | 4 | HIGH | updateMerch, getOwnMerchItem, deleteMerch, updateMerch |
| `merch/service/MerchService.java:resolveCategory` | 2 | 4 | HIGH | createMerch, updateMerch, createMerch, updateMerch |
| `merch/service/MerchService.java:buildCategoryMap` | 5 | 6 | CRITICAL | visualSearch, getPopular, listMerch, getOwnMerch, getOrgMerch, vectorSearch |
| `merch/service/MerchService.java:popularityScore` | 1 | 1 | LOW | getPopular |
| `merch/service/MerchService.java:toOrderCountMap` | 1 | 1 | LOW | getPopular |
| `merch/service/MerchService.java:embeddingText` | 2 | 4 | HIGH | createMerch, updateMerch, createMerch, updateMerch |
| `event/service/EventService.java:EventService` | 4 | 0 | LOW |  |
| `event/service/EventService.java:createEvent` | 1 | 1 | LOW | createEvent |
| `event/service/EventService.java:getOwnEvents` | 1 | 1 | LOW | getOwnEvents |
| `event/service/EventService.java:getOwnEvent` | 1 | 1 | LOW | getOwnEvent |
| `event/service/EventService.java:updateEvent` | 1 | 1 | LOW | updateEvent |
| `event/service/EventService.java:deleteEvent` | 1 | 1 | LOW | deleteEvent |
| `event/service/EventService.java:attachMerch` | 1 | 1 | LOW | attachMerch |
| `event/service/EventService.java:detachMerch` | 1 | 1 | LOW | detachMerch |
| `event/service/EventService.java:getPublicEvents` | 1 | 1 | LOW | getPublicEvents |
| `event/service/EventService.java:getPublicEventsByOrg` | 1 | 1 | LOW | getOrgEvents |
| `event/service/EventService.java:getPublicEvent` | 1 | 1 | LOW | getPublicEvent |
| `event/service/EventService.java:fetchMerchForEvent` | 3 | 3 | HIGH | getPublicEvent, attachMerch, getOwnEvent |
| `event/service/EventService.java:validateStatusTransition` | 1 | 1 | LOW | updateEvent |
| `event/service/EventService.java:validateDates` | 2 | 2 | LOW | updateEvent, createEvent |
| `common/config/JwtAuthenticationFilter.java:JwtAuthenticationFilter` | 2 | 0 | LOW |  |
| `common/config/JwtAuthenticationFilter.java:doFilterInternal` | 0 | 0 | UNKNOWN |  |
| `common/config/JwtAuthenticationFilter.java:extractToken` | 1 | 1 | LOW | doFilterInternal |
| `common/config/SecurityConfig.java:SecurityConfig` | 0 | 0 | UNKNOWN |  |
| `common/security/JwtTokenProvider.java:JwtTokenProvider` | 3 | 0 | LOW |  |
| `common/security/JwtTokenProvider.java:validateSecretKey` | 0 | 0 | UNKNOWN |  |
| `common/security/JwtTokenProvider.java:generateAccessToken` | 2 | 2 | HIGH | login, refresh |
| `common/security/JwtTokenProvider.java:generateRefreshToken` | 2 | 2 | HIGH | login, refresh |
| `common/security/JwtTokenProvider.java:validateToken` | 3 | 4 | HIGH | logout, refresh, login, doFilterInternal |
| `common/security/JwtTokenProvider.java:validateAsRefreshToken` | 1 | 2 | HIGH | login, refresh |
| `common/security/JwtTokenProvider.java:getUserIdFromToken` | 2 | 3 | HIGH | login, refresh, doFilterInternal |
| `common/security/JwtTokenProvider.java:getEmailFromToken` | 1 | 1 | LOW | doFilterInternal |
| `common/security/JwtTokenProvider.java:getRoleFromToken` | 1 | 1 | LOW | doFilterInternal |
| `common/security/JwtTokenProvider.java:getExpiryFromToken` | 2 | 3 | HIGH | login, logout, refresh |
| `common/security/JwtTokenProvider.java:getAccessTokenExpiration` | 0 | 0 | UNKNOWN |  |
| `common/security/JwtTokenProvider.java:getAccessTokenExpiryInstant` | 0 | 0 | UNKNOWN |  |
| `common/security/JwtTokenProvider.java:generateToken` | 2 | 2 | HIGH | login, refresh |
| `common/security/JwtTokenProvider.java:getClaim` | 4 | 3 | HIGH | refresh, login, doFilterInternal |
| `common/security/JwtTokenProvider.java:getAllClaims` | 2 | 4 | HIGH | refresh, logout, login, doFilterInternal |
| `common/security/JwtTokenProvider.java:getSigningKey` | 3 | 4 | HIGH | logout, login, refresh, doFilterInternal |
| `common/service/JavaMailEmailService.java:JavaMailEmailService` | 1 | 0 | LOW |  |
| `common/service/JavaMailEmailService.java:sendOtp` | 1 | 3 | HIGH | resendOtp, register, registerOrganizer |
| `common/service/JavaMailEmailService.java:sendPasswordReset` | 1 | 1 | LOW | forgotPassword |
| `common/service/JavaMailEmailService.java:sendOrderPlacedConfirmation` | 1 | 3 | HIGH | checkout, createInstantOrder, notifyCustomerOrderPlaced |
| `common/service/JavaMailEmailService.java:sendOrderStatusUpdate` | 1 | 2 | LOW | cancelOrder, checkIn |
| `common/service/JavaMailEmailService.java:sendPickupScheduleNotification` | 1 | 2 | LOW | sendPickupNotification, createPickupSchedule |
| `common/service/JavaMailEmailService.java:sendOrderCancelledNotification` | 2 | 4 | HIGH | cancelOrder, cancelOrder, notifyOrgOwnerOfCancel, sendCancelEmail |
| `common/service/JavaMailEmailService.java:sendMail` | 6 | 14 | CRITICAL | resendOtp, register, registerOrganizer, forgotPassword, checkout, notifyCustomerOrderPlaced, sendPickupNotification, cancelOrder, createInstantOrder, cancelOrder, notifyOrgOwnerOfCancel, sendCancelEmail, checkIn, createPickupSchedule |
| `common/service/JavaMailEmailService.java:buildOtpHtml` | 2 | 4 | HIGH | forgotPassword, register, registerOrganizer, resendOtp |
| `common/service/JavaMailEmailService.java:buildHtml` | 1 | 3 | HIGH | resendOtp, register, registerOrganizer |
| `common/service/TokenBlacklistService.java:TokenBlacklistService` | 4 | 0 | LOW |  |
| `common/service/TokenBlacklistService.java:loadFromDatabase` | 0 | 0 | UNKNOWN |  |
| `common/service/TokenBlacklistService.java:add` | 2 | 3 | HIGH | login, logout, refresh |
| `common/service/TokenBlacklistService.java:isBlacklisted` | 2 | 3 | HIGH | login, refresh, doFilterInternal |
| `common/service/TokenBlacklistService.java:evictExpired` | 0 | 0 | UNKNOWN |  |
| `common/service/TokenBlacklistService.java:sha256` | 2 | 4 | HIGH | logout, refresh, login, doFilterInternal |
| `cart/service/CartService.java:CartService` | 2 | 0 | LOW |  |
| `cart/service/CartService.java:getCart` | 1 | 1 | LOW | getCart |
| `cart/service/CartService.java:addItem` | 1 | 1 | LOW | addItem |
| `cart/service/CartService.java:updateItem` | 1 | 1 | LOW | updateItem |
| `cart/service/CartService.java:removeItem` | 1 | 1 | LOW | removeItem |
| `cart/service/CartService.java:checkout` | 1 | 1 | LOW | checkout |
| `cart/service/CartService.java:findOrCreateActiveCart` | 2 | 2 | LOW | getCart, addItem |
| `cart/service/CartService.java:getActiveCartOrThrow` | 3 | 3 | HIGH | checkout, updateItem, removeItem |
| `cart/service/CartService.java:buildCartResponse` | 3 | 3 | HIGH | getCart, updateItem, addItem |
| `auth/controller/AuthController.java:AuthController` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:register` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:registerOrganizer` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:verifyEmail` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:login` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:refresh` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:resendOtp` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:forgotPassword` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:resetPassword` | 0 | 0 | UNKNOWN |  |
| `auth/controller/AuthController.java:logout` | 0 | 0 | UNKNOWN |  |
| `auth/entity/OtpToken.java:OtpToken` | 3 | 0 | LOW |  |
| `auth/entity/User.java:User` | 9 | 0 | MEDIUM |  |
| `auth/repository/UserRepository.java:UserRepository` | 6 | 0 | MEDIUM |  |
| `auth/service/AuthService.java:AuthService` | 2 | 0 | LOW |  |
| `auth/service/AuthService.java:register` | 1 | 1 | LOW | register |
| `auth/service/AuthService.java:registerOrganizer` | 1 | 1 | LOW | registerOrganizer |
| `auth/service/AuthService.java:verifyEmail` | 1 | 1 | LOW | verifyEmail |
| `auth/service/AuthService.java:login` | 1 | 1 | LOW | login |
| `auth/service/AuthService.java:refreshToken` | 2 | 2 | HIGH | login, refresh |
| `auth/service/AuthService.java:logout` | 1 | 1 | LOW | logout |
| `auth/service/AuthService.java:registerWithRole` | 2 | 2 | LOW | register, registerOrganizer |
| `auth/service/AuthService.java:validatePassword` | 2 | 3 | HIGH | register, registerOrganizer, resetPassword |
| `auth/service/AuthService.java:issueOtp` | 2 | 3 | HIGH | register, registerOrganizer, resendOtp |
| `auth/service/AuthService.java:generateOtpCode` | 2 | 4 | HIGH | resendOtp, register, registerOrganizer, forgotPassword |
| `auth/service/AuthService.java:resendOtp` | 1 | 1 | LOW | resendOtp |
| `auth/service/AuthService.java:forgotPassword` | 1 | 1 | LOW | forgotPassword |
| `auth/service/AuthService.java:resetPassword` | 1 | 1 | LOW | resetPassword |
| `auth/service/AuthService.java:purgeExpiredOtps` | 0 | 0 | UNKNOWN |  |
| `ai/controller/AiMerchSearchController.java:AiMerchSearchController` | 1 | 0 | LOW |  |
| `ai/controller/AiMerchSearchController.java:visualSearch` | 0 | 0 | UNKNOWN |  |
| `ai/service/GeminiEmbeddingService.java:GeminiEmbeddingService` | 1 | 0 | LOW |  |
| `ai/service/GeminiEmbeddingService.java:embed` | 3 | 6 | CRITICAL | createMerch, updateMerch, visualSearch, vectorSearch, createMerch, updateMerch |
| `ai/service/GeminiEmbeddingService.java:callWithRotation` | 1 | 6 | CRITICAL | createMerch, updateMerch, vectorSearch, visualSearch, createMerch, updateMerch |
| `ai/service/GeminiVisionAiService.java:GeminiVisionAiService` | 1 | 0 | LOW |  |
| `ai/service/GeminiVisionAiService.java:describeImage` | 1 | 1 | LOW | visualSearch |
| `ai/service/GeminiVisionAiService.java:callWithRotation` | 1 | 1 | LOW | visualSearch |
| `ai/service/ProdMerchEmbeddingService.java:ProdMerchEmbeddingService` | 1 | 0 | LOW |  |
| `ai/service/ProdMerchEmbeddingService.java:storeAsync` | 2 | 4 | HIGH | createMerch, updateMerch, createMerch, updateMerch |
| `ai/service/ProdMerchEmbeddingService.java:store` | 2 | 4 | HIGH | createMerch, updateMerch, createMerch, updateMerch |
| `ai/service/ProdMerchEmbeddingService.java:findNearest` | 1 | 2 | LOW | visualSearch, vectorSearch |
| `ai/service/ProdMerchEmbeddingService.java:findAllMerchIdsWithEmbedding` | 1 | 0 | LOW |  |
| `ai/service/ProdMerchEmbeddingService.java:toVectorString` | 2 | 6 | CRITICAL | createMerch, updateMerch, visualSearch, vectorSearch, createMerch, updateMerch |
| `ai/service/VisualSearchService.java:VisualSearchService` | 1 | 0 | LOW |  |
| `ai/service/VisualSearchService.java:search` | 1 | 1 | LOW | visualSearch |
| `ai/service/VisualSearchService.java:vectorSearch` | 1 | 1 | LOW | visualSearch |
| `ai/service/VisualSearchService.java:keywordFallback` | 1 | 1 | LOW | visualSearch |
| `ai/service/VisualSearchService.java:validateFile` | 1 | 1 | LOW | visualSearch |
| `admin/service/AdminService.java:AdminService` | 2 | 0 | LOW |  |
| `admin/service/AdminService.java:listUsers` | 1 | 1 | LOW | listUsers |
| `admin/service/AdminService.java:updateUserRole` | 1 | 1 | LOW | updateUserRole |
| `admin/service/AdminService.java:setUserActive` | 1 | 0 | LOW |  |
| `admin/service/AdminService.java:listOrganizations` | 1 | 1 | LOW | listOrganizations |
| `admin/service/AdminService.java:updateOrganizationStatus` | 1 | 1 | LOW | updateOrganizationStatus |
| `admin/service/AdminService.java:listAllOrders` | 1 | 1 | LOW | listAllOrders |
| `admin/service/AdminService.java:batchCountPublishedMerch` | 1 | 1 | LOW | listOrganizations |

Additional repository interface and mail-transport checks:

- `wishlist/repository/WishlistRepository.java:WishlistRepository`: LOW; 2 direct callers; 0 process groups.
- `wishlist/repository/WishlistRepository.java:findByUserId`: HIGH; 1 direct callers; 3 process groups.
- `order/repository/OrderRepository.java:OrderRepository`: LOW; 3 direct callers; 0 process groups.
- `order/repository/OrderRepository.java:findByUserId`: LOW; 1 direct callers; 1 process groups.
- `order/repository/OrderRepository.java:findByUserIdAndStatus`: LOW; 1 direct callers; 1 process groups.
- `order/repository/OrderRepository.java:findByOrgId`: LOW; 1 direct callers; 1 process groups.
- `order/repository/OrderRepository.java:findByOrgIdAndStatus`: LOW; 1 direct callers; 1 process groups.
- `order/repository/OrderRepository.java:findByStatus`: LOW; 1 direct callers; 1 process groups.
- `order/repository/OrderRepository.java:findByPickupScheduleId`: LOW; 1 direct callers; 1 process groups.
- `order/repository/OrderRepository.java:countByPickupScheduleId`: LOW; 1 direct callers; 1 process groups.
- `merch/repository/MerchItemRepository.java:MerchItemRepository`: MEDIUM; 10 direct callers; 0 process groups.
- `merch/repository/MerchItemRepository.java:findByStatus`: HIGH; 1 direct callers; 2 process groups.
- `merch/repository/MerchItemRepository.java:findAllByStatus`: UNKNOWN; unknown direct callers; unknown process groups.
- `merch/repository/MerchItemRepository.java:findByStatusAndNameContainingIgnoreCase`: HIGH; 1 direct callers; 2 process groups.
- `merch/repository/MerchItemRepository.java:findByStatusAndCategoryId`: HIGH; 1 direct callers; 2 process groups.
- `merch/repository/MerchItemRepository.java:findByStatusAndCategoryIdAndNameContainingIgnoreCase`: HIGH; 1 direct callers; 2 process groups.
- `merch/repository/MerchItemRepository.java:findByOrgId`: LOW; 1 direct callers; 1 process groups.
- `merch/repository/MerchItemRepository.java:findByOrgIdAndStatus`: LOW; 1 direct callers; 1 process groups.
- `merch/repository/MerchItemRepository.java:findByIdAndOrgId`: HIGH; 1 direct callers; 4 process groups.
- `merch/repository/MerchItemRepository.java:existsByIdAndOrgId`: LOW; 1 direct callers; 1 process groups.
- `merch/repository/MerchItemRepository.java:countByOrgIdAndStatus`: HIGH; 2 direct callers; 3 process groups.
- `merch/repository/MerchItemRepository.java:countByOrgIdsAndStatus`: HIGH; 2 direct callers; 3 process groups.
- `merch/repository/MerchItemRepository.java:deductStock`: HIGH; 3 direct callers; 3 process groups.
- `merch/repository/MerchItemRepository.java:archivePublishedByOrgId`: LOW; 1 direct callers; 1 process groups.
- `merch/repository/MerchItemRepository.java:restoreStock`: LOW; 1 direct callers; 2 process groups.
- `event/repository/EventRepository.java:EventRepository`: LOW; 2 direct callers; 0 process groups.
- `event/repository/EventRepository.java:findByOrgId`: LOW; 1 direct callers; 1 process groups.
- `event/repository/EventRepository.java:findByStatus`: UNKNOWN; 0 direct callers; 0 process groups.
- `event/repository/EventRepository.java:findByStatusIn`: LOW; 1 direct callers; 1 process groups.
- `event/repository/EventRepository.java:findByOrgIdAndStatus`: UNKNOWN; 0 direct callers; 0 process groups.
- `event/repository/EventRepository.java:findByOrgIdAndStatusIn`: LOW; 1 direct callers; 1 process groups.
- `event/repository/EventRepository.java:findByIdAndOrgId`: CRITICAL; 5 direct callers; 5 process groups.
- `common/service/DevEmailService.java:DevEmailService`: UNKNOWN; 0 direct callers; 0 process groups.
- `cart/repository/CartRepository.java:CartRepository`: LOW; 2 direct callers; 0 process groups.
- `cart/repository/CartRepository.java:findByUserId`: LOW; 1 direct callers; 2 process groups.
- `cart/repository/CartRepository.java:findByUserIdAndStatus`: CRITICAL; 2 direct callers; 5 process groups.
- `auth/repository/UserRepository.java:UserRepository`: MEDIUM; 6 direct callers; 0 process groups.
- `auth/repository/UserRepository.java:findByEmail`: CRITICAL; 6 direct callers; 5 process groups.
- `auth/repository/UserRepository.java:existsByEmail`: LOW; 1 direct callers; 2 process groups.
- `auth/repository/UserRepository.java:findByRole`: LOW; 1 direct callers; 1 process groups.

Additional implementation checks covered the new public eligibility query (HIGH: 3 callers, 4 groups); SSE registration/removal (HIGH), delivery (CRITICAL), and reflected stream endpoints (UNKNOWN); the exception handlers and startup backfill (UNKNOWN reflected entry points); and the blacklist insert (HIGH: 1 caller, 3 groups). The paged findAllByStatus overload was resolved by symbol UID (LOW: 1 caller, 1 group). Source inspection and controller/integration tests complement graph omissions.

Final reindex and change detection: 44 tracked files, 251 changed symbols, 164 affected execution flows; overall CRITICAL impact. The index reports capped process traversal and unresolved cross-language fields. Change detection covers tracked diffs; the validation manifest also includes added backend files. The final scope is backend source, configuration, migrations, tests, the test harness, and review documents. Analyzer-generated instruction/skill relocation changes were reverted. No frontend source changes or commits were made.
