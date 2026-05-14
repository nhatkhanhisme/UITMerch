import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { HomeFixedChrome } from "./components/home/HomeFixedChrome";
import { HomePage } from "./pages/HomePage";
import { MerchPage } from "./pages/MerchPage";
import { ProductDetailPage } from "./pages/ProductDetailPage";
import { AuthPage } from "./pages/AuthPage";
import { ProfilePage } from "./pages/ProfilePage";
import { OrganizerDashboardPage } from "./pages/OrganizerDashboardPage";
import { OrganizationDetailPage } from "./pages/OrganizationDetailPage";
import { OrganizationPage } from "./pages/OrganizationPage";
import "./styles.css";

function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<HomePage />} path="/" />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <MerchPage />
            </>
          }
          path="/merch"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <ProductDetailPage />
            </>
          }
          path="/merch/:id"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <AuthPage />
            </>
          }
          path="/auth"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <ProfilePage />
            </>
          }
          path="/profile"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <OrganizationPage />
            </>
          }
          path="/organization"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <OrganizationPage />
            </>
          }
          path="/organizations"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <OrganizationDetailPage />
            </>
          }
          path="/organization/:id"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <OrganizationDetailPage />
            </>
          }
          path="/organizations/:id"
        />
        <Route
          element={
            <>
              <HomeFixedChrome showSlideBar={false} />
              <OrganizerDashboardPage />
            </>
          }
          path="/organizer"
        />
        <Route element={<Navigate replace to="/" />} path="*" />
      </Routes>
    </BrowserRouter>
  );
}

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <AppRouter />
  </React.StrictMode>,
);
