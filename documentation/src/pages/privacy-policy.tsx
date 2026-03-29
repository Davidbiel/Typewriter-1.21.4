import React from "react";
import Link from "@docusaurus/Link";
import "../css/custom.css";

const PrivacyPolicy = () => {
  return (
    <div className="privacy-policy-container" style={{ padding: "40px", maxWidth: "800px", margin: "0 auto" }}>
      <h1 style={{ fontSize: "2.5em", marginBottom: "20px", textAlign: "center" }}>
        Privacy Policy
      </h1>
      <p style={{ marginBottom: "20px", fontSize: "1.2em", lineHeight: "1.6" }}>
        We respect your privacy and are committed to protecting it. This privacy policy lets you know how we collect and use your personal information.
      </p>
      <p style={{ marginBottom: "20px", fontSize: "1.2em", lineHeight: "1.6" }}>
        We do not share personal information with third-parties except as described in this policy. We collect information about your visit to this documentation site to analyze content performance through the use of cookies, which you can turn off at any time by modifying your Internet browser's settings.
      </p>
      <h2 style={{ fontSize: "2em", marginBottom: "15px" }}>Data Collection</h2>
      <p style={{ marginBottom: "20px", fontSize: "1.2em", lineHeight: "1.6" }}>
        We use the following tools that may collect data:
      </p>
      <ul style={{ marginBottom: "20px", fontSize: "1.2em", lineHeight: "1.6", paddingLeft: "20px" }}>
        <li style={{ marginBottom: "10px" }}>
          <strong>PostHog:</strong> We use PostHog to collect data on user interactions, such as page views, click events, session duration, referrer information, and user agent details (browser, OS, device type), to help us understand how users engage with our documentation.
        </li>
        <li style={{ marginBottom: "10px" }}>
          <strong>Algolia:</strong> We use Algolia to provide fast and relevant search results. Algolia collects data on search queries, click-through rates, search result rankings, user interactions with search results, and metadata about the documents being searched to improve the search experience.
        </li>
      </ul>
      <p style={{ marginBottom: "20px", fontSize: "1.2em", lineHeight: "1.6" }}>
        We are not responsible for the republishing of the content found on this documentation site on other websites or media without our permission.
      </p>
      <p style={{ marginBottom: "20px", fontSize: "1.2em", lineHeight: "1.6" }}>
        This privacy policy is subject to change without notice.
      </p>
      <Link to="/">
        Go to Homepage
      </Link>
    </div>
  );
};

export default PrivacyPolicy;