import React from "react";
import Link from "@docusaurus/Link";
import "../css/custom.css";

const LeavePage = () => {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        textAlign: "center",
        padding: "20px",
      }}
      className="leave-container"
    >
      <h1 style={{ fontSize: "2em", marginBottom: "20px" }}>
        You've Chosen to Leave
      </h1>
      <p style={{ marginBottom: "10px" }}>
        We respect your decision to decline cookies. Unfortunately, this website
        requires cookies to function properly.
      </p>
      <p style={{ marginBottom: "20px" }}>
        If you wish to return, you can go back to the homepage and allow us to
        use cookies.
      </p>
      <Link to="/" style={{ fontSize: "1.2em" }}>
        Go to Homepage
      </Link>
    </div>
  );
};

export default LeavePage;
