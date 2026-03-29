import React, { ReactNode } from "react";
import clsx from "clsx";

type DeopasityProps = {
  children: ReactNode;
  className?: string;
  default?: number;
  hover?: number;
};

/**
 * A component that applies opacity effects to its children.
 * By default, content has reduced opacity and becomes fully opaque on hover.
 * @param default - Default opacity value (0-1), defaults to 0.9
 * @param hover - Hover opacity value (0-1), defaults to 1
 */
export default function Deopasity({
  children,
  className,
  default: defaultOpacity = 0.9,
  hover: hoverOpacity = 1,
}: DeopasityProps): React.ReactElement {
  const style = {
    "--default-opacity": defaultOpacity,
    "--hover-opacity": hoverOpacity,
  } as React.CSSProperties;

  return (
    <div
      className={clsx(
        "transition-opacity duration-300 ease-in-out",
        "opacity-[var(--default-opacity)]",
        "hover:opacity-[var(--hover-opacity)]",
        className
      )}
      style={style}
    >
      {children}
    </div>
  );
}
