import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { fetchPublicBranding, type PublicBranding } from '@/api/public';
import {
  DOWNLOAD_PAGE_DESCRIPTION,
  DOWNLOAD_PAGE_TITLE,
  SYSTEM_NAME,
  SYSTEM_SHORT_NAME,
  SYSTEM_SUBTITLE,
} from '@/config/branding';
import { resolveAssetUrl } from '@/utils/app';

interface BrandingContextValue {
  branding: PublicBranding;
  loading: boolean;
  refresh: () => Promise<void>;
  logoSrc?: string;
  faviconSrc?: string;
}

const defaultBranding: PublicBranding = {
  siteName: SYSTEM_NAME,
  siteShortName: SYSTEM_SHORT_NAME,
  siteSubtitle: SYSTEM_SUBTITLE,
  primaryColor: '#1677ff',
  loginBackground: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  downloadPageTitle: DOWNLOAD_PAGE_TITLE,
  downloadPageDescription: DOWNLOAD_PAGE_DESCRIPTION,
  defaultServerUrl: 'http://124.220.4.69:5555',
};

const BrandingContext = createContext<BrandingContextValue>({
  branding: defaultBranding,
  loading: true,
  refresh: async () => {},
});

export function BrandingProvider({ children }: { children: ReactNode }) {
  const [branding, setBranding] = useState<PublicBranding>(defaultBranding);
  const [loading, setLoading] = useState(true);

  const refresh = async () => {
    try {
      const data = await fetchPublicBranding();
      setBranding({ ...defaultBranding, ...data });
    } catch {
      setBranding(defaultBranding);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refresh();
  }, []);

  useEffect(() => {
    const color = branding.primaryColor || defaultBranding.primaryColor;
    if (color) {
      document.documentElement.style.setProperty('--uqm-primary', color);
    }
    const favicon = resolveAssetUrl(branding.faviconUrl);
    if (favicon) {
      let link = document.querySelector<HTMLLinkElement>("link[rel='icon']");
      if (!link) {
        link = document.createElement('link');
        link.rel = 'icon';
        document.head.appendChild(link);
      }
      link.href = favicon;
    }
  }, [branding]);

  const value = useMemo(
    () => ({
      branding,
      loading,
      refresh,
      logoSrc: resolveAssetUrl(branding.logoUrl),
      faviconSrc: resolveAssetUrl(branding.faviconUrl),
    }),
    [branding, loading]
  );

  return <BrandingContext.Provider value={value}>{children}</BrandingContext.Provider>;
}

export function useBranding() {
  return useContext(BrandingContext);
}
