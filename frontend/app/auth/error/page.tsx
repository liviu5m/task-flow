"use client";

import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { Suspense } from "react";

const errorMessages: Record<string, { title: string; description: string }> = {
  invalid_callback_request: {
    title: "Invalid Callback Request",
    description:
      "The authentication request callback was malformed or unrecognized.",
  },
  invalid_code: {
    title: "Invalid Authorization Code",
    description:
      "The authorization code provided by the provider is invalid or has expired.",
  },
  internal_server_error: {
    title: "Internal Server Error",
    description:
      "An unexpected error occurred on our servers during authentication.",
  },
  state_not_found: {
    title: "State Not Found",
    description:
      "The security state cookie could not be found. Please try logging in again.",
  },
  state_invalid: {
    title: "Invalid State Token",
    description: "The security state token failed validation.",
  },
  state_mismatch: {
    title: "State Mismatch",
    description:
      "The security validation failed due to a mismatched session state. Try clearing your cookies or opening an incognito window.",
  },
  no_code: {
    title: "Missing Authorization Code",
    description:
      "No authorization code was returned from the identity provider.",
  },
  no_callback_url: {
    title: "Missing Callback URL",
    description: "The target callback redirect URL was not specified.",
  },
  oauth_provider_not_found: {
    title: "Provider Not Found",
    description:
      "The selected authentication provider is not supported or configured correctly.",
  },
  email_not_found: {
    title: "Email Not Found",
    description:
      "Could not retrieve your email address from the authentication provider.",
  },
  "email_doesn't_match": {
    title: "Email Mismatch",
    description:
      "The email address associated with your account does not match the provider's email.",
  },
  unable_to_get_user_info: {
    title: "Failed to Fetch Profile",
    description:
      "We couldn't retrieve your profile information from the provider.",
  },
  unable_to_link_account: {
    title: "Account Linking Failed",
    description:
      "Could not link this authentication method to your existing user account.",
  },
  unable_to_create_user: {
    title: "User Creation Failed",
    description:
      "We encountered an error while trying to create your user profile.",
  },
  unable_to_create_session: {
    title: "Session Creation Failed",
    description:
      "Your authentication was successful, but we couldn't establish your login session.",
  },
  account_not_linked: {
    title: "Account Not Linked",
    description:
      "Please sign in using the original method you used to create your account.",
  },
  account_already_linked_to_different_user: {
    title: "Account Already Linked",
    description:
      "This provider account is already connected to a different profile.",
  },
  signup_disabled: {
    title: "Sign Ups Disabled",
    description: "New account registrations are currently turned off.",
  },
};

function AuthErrorContent() {
  const searchParams = useSearchParams();
  const errorCode = searchParams.get("error") || "internal_server_error";

  const currentError = errorMessages[errorCode] || {
    title: "Authentication Error",
    description:
      "An unexpected authentication error occurred. Please try again.",
  };

  return (
    <div className="min-h-screen bg-[#0f172a] flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-[#1e293b] border border-[#334155] rounded-2xl p-8 shadow-xl text-center">
        <div className="w-12 h-12 bg-red-500/10 border border-red-500/20 rounded-xl flex items-center justify-center mx-auto mb-6 text-red-400 font-bold text-xl">
          !
        </div>

        <h1 className="text-xl font-semibold text-[#f8fafc] mb-2">
          {currentError.title}
        </h1>

        <p className="text-sm text-[#94a3b8] mb-6 leading-relaxed">
          {currentError.description}
        </p>

        <div className="bg-[#0f172a] border border-[#334155] rounded-lg p-3 mb-6 text-xs text-[#64748b] font-mono break-all">
          error_code: {errorCode}
        </div>

        <div className="flex gap-3">
          <Link
            href="/auth/login"
            className="flex-1 bg-[#4f46e5] hover:bg-[#4338ca] text-white text-sm font-medium py-2.5 px-4 rounded-xl transition-colors text-center"
          >
            Back to Login
          </Link>
          <Link
            href="/"
            className="bg-[#334155] hover:bg-[#475569] text-[#f8fafc] text-sm font-medium py-2.5 px-4 rounded-xl transition-colors text-center"
          >
            Home
          </Link>
        </div>
      </div>
    </div>
  );
}

export default function AuthErrorPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-[#0f172a]" />}>
      <AuthErrorContent />
    </Suspense>
  );
}
