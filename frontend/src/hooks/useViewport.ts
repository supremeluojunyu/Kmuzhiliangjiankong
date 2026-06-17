import { useEffect, useState } from 'react';

export interface ViewportSize {
  width: number;
  height: number;
  isNarrow: boolean;
  isShort: boolean;
  isCompact: boolean;
}

function readViewport(): ViewportSize {
  const width = window.innerWidth;
  const height = window.innerHeight;
  const isNarrow = width < 992;
  const isShort = height < 720;
  return {
    width,
    height,
    isNarrow,
    isShort,
    isCompact: isNarrow || isShort,
  };
}

/** 监听窗口尺寸变化，供布局与菜单自适应 */
export function useViewport(): ViewportSize {
  const [viewport, setViewport] = useState(readViewport);

  useEffect(() => {
    const onResize = () => setViewport(readViewport());
    window.addEventListener('resize', onResize, { passive: true });
    window.visualViewport?.addEventListener('resize', onResize, { passive: true });
    return () => {
      window.removeEventListener('resize', onResize);
      window.visualViewport?.removeEventListener('resize', onResize);
    };
  }, []);

  return viewport;
}
