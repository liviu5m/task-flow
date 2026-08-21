import { betterAuth } from "better-auth";
import { Pool } from "pg";
import { sendEmailVerification } from "./resend";

export const auth = betterAuth({
  database: new Pool({
    connectionString: process.env.DATABASE_URL,
  }),
  emailAndPassword: {
    enabled: true,
    requireEmailVerification: true,
  },
  socialProviders: {
    google: {
      clientId: process.env.GOOGLE_CLIENT_ID as string,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET,
    },
  },
  emailVerification: {
    sendOnSignUp: true,
    sendVerificationEmail: async ({ user, url, token }) => {
      await sendEmailVerification(user, url);
    },
  },
  onAPIError: {
    errorURL: "/auth/error",
  },
});
