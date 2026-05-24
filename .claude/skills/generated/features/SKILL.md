---
name: features
description: "Skill for the Features area of UITMerch. 9 symbols across 5 files."
---

# Features

9 symbols | 5 files | Cohesion: 56%

## When to Use

- Working with code in `frontend/`
- Understanding how FeaturedSlider, ProductGrid, ProductCard work
- Modifying features-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `frontend/src/components/features/MerchToolbar.tsx` | SearchIcon, ChevronDownIcon, Dropdown, MerchToolbar |
| `frontend/src/pages/MerchPage.tsx` | ShaderBackground, MerchPage |
| `frontend/src/components/features/FeaturedSlider.tsx` | FeaturedSlider |
| `frontend/src/components/features/ProductGrid.tsx` | ProductGrid |
| `frontend/src/components/ui/ProductCard.tsx` | ProductCard |

## Entry Points

Start here when exploring this area:

- **`FeaturedSlider`** (Function) — `frontend/src/components/features/FeaturedSlider.tsx:18`
- **`ProductGrid`** (Function) — `frontend/src/components/features/ProductGrid.tsx:8`
- **`ProductCard`** (Function) — `frontend/src/components/ui/ProductCard.tsx:19`
- **`MerchPage`** (Function) — `frontend/src/pages/MerchPage.tsx:37`
- **`MerchToolbar`** (Function) — `frontend/src/components/features/MerchToolbar.tsx:137`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `FeaturedSlider` | Function | `frontend/src/components/features/FeaturedSlider.tsx` | 18 |
| `ProductGrid` | Function | `frontend/src/components/features/ProductGrid.tsx` | 8 |
| `ProductCard` | Function | `frontend/src/components/ui/ProductCard.tsx` | 19 |
| `MerchPage` | Function | `frontend/src/pages/MerchPage.tsx` | 37 |
| `MerchToolbar` | Function | `frontend/src/components/features/MerchToolbar.tsx` | 137 |
| `ShaderBackground` | Function | `frontend/src/pages/MerchPage.tsx` | 14 |
| `SearchIcon` | Function | `frontend/src/components/features/MerchToolbar.tsx` | 18 |
| `ChevronDownIcon` | Function | `frontend/src/components/features/MerchToolbar.tsx` | 27 |
| `Dropdown` | Function | `frontend/src/components/features/MerchToolbar.tsx` | 42 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `MerchPage → BuildKey` | cross_community | 4 |
| `EventPage → ChevronDownIcon` | cross_community | 4 |
| `OrganizationPage → ChevronDownIcon` | cross_community | 4 |
| `MerchPage → GetPublicOrganizations` | cross_community | 3 |
| `MerchPage → GetCategories` | cross_community | 3 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Pages | 3 calls |

## How to Explore

1. `gitnexus_context({name: "FeaturedSlider"})` — see callers and callees
2. `gitnexus_query({query: "features"})` — find related execution flows
3. Read key files listed above for implementation details
