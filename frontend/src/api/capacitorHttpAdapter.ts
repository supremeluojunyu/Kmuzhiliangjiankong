import { CapacitorHttp, type HttpResponse } from '@capacitor/core';
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios';

function buildFullUrl(config: InternalAxiosRequestConfig): string {
  const base = config.baseURL || '';
  const path = config.url || '';
  if (/^https?:\/\//i.test(path)) return path;
  if (!base) return path;
  return `${base.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
}

/** 不依赖 AxiosHeaders.forEach（Android WebView 中可能不可用） */
function headersToRecord(config: InternalAxiosRequestConfig): Record<string, string> {
  const out: Record<string, string> = {};
  const raw = config.headers;
  if (!raw || typeof raw !== 'object') return out;

  let source: Record<string, unknown>;
  if (typeof (raw as { toJSON?: () => unknown }).toJSON === 'function') {
    source = (raw as { toJSON: () => Record<string, unknown> }).toJSON();
  } else {
    source = raw as Record<string, unknown>;
  }

  for (const key of Object.keys(source)) {
    const value = source[key];
    if (value === undefined || value === null || typeof value === 'function') continue;
    if (Array.isArray(value)) {
      out[key] = value.map(String).join(', ');
    } else {
      out[key] = String(value);
    }
  }
  return out;
}

function prepareBody(
  data: unknown,
  headers: Record<string, string>
): string | undefined {
  if (data === undefined || data === null) return undefined;
  if (typeof data === 'string') return data;
  if (typeof FormData !== 'undefined' && data instanceof FormData) {
    throw new Error('FormData 上传请使用专用接口');
  }
  if (!headers['Content-Type'] && !headers['content-type']) {
    headers['Content-Type'] = 'application/json';
  }
  return JSON.stringify(data);
}

function parseResponseData(data: HttpResponse['data']): unknown {
  if (typeof data !== 'string') return data;
  try {
    return JSON.parse(data);
  } catch {
    return data;
  }
}

/** 原生 HTTP 适配器，绕过 WebView 对 http 混合内容的拦截 */
export const capacitorHttpAdapter: AxiosAdapter = async (config) => {
  const url = buildFullUrl(config);
  const method = (config.method?.toUpperCase() || 'GET') as
    | 'GET'
    | 'POST'
    | 'PUT'
    | 'DELETE'
    | 'PATCH';
  const headers = headersToRecord(config);
  const data =
    method === 'GET' || method === 'DELETE' ? undefined : prepareBody(config.data, headers);

  const response = await CapacitorHttp.request({
    url,
    method,
    headers,
    data,
    connectTimeout: config.timeout ?? 30000,
    readTimeout: config.timeout ?? 30000,
  });

  return {
    data: parseResponseData(response.data),
    status: response.status,
    statusText: '',
    headers: (response.headers || {}) as Record<string, string>,
    config,
    request: { responseURL: url },
  };
};

export async function nativeHttpRequest<T = unknown>(options: {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  headers?: Record<string, string>;
  data?: unknown;
  timeout?: number;
}): Promise<T> {
  const headers: Record<string, string> = { ...(options.headers || {}) };
  const method = options.method || 'GET';
  const data = method === 'GET' || method === 'DELETE'
    ? undefined
    : prepareBody(options.data, headers);

  const response = await CapacitorHttp.request({
    url: options.url,
    method,
    headers,
    data,
    connectTimeout: options.timeout ?? 12000,
    readTimeout: options.timeout ?? 12000,
  });

  const body = parseResponseData(response.data);

  if (typeof body === 'object' && body !== null && 'code' in body) {
    const apiBody = body as { code?: number; message?: string };
    if (apiBody.code !== 0) {
      throw new Error(apiBody.message || '请求失败');
    }
  }

  if (response.status < 200 || response.status >= 300) {
    const msg =
      typeof body === 'object' && body && 'message' in body && (body as { message?: string }).message
        ? (body as { message: string }).message
        : `HTTP ${response.status}`;
    throw new Error(msg);
  }

  return body as T;
}

export async function nativeHttpGet(
  url: string,
  headers: Record<string, string>
): Promise<{ status: number; data: string; headers: Record<string, string> }> {
  const response = await CapacitorHttp.request({
    url,
    method: 'GET',
    headers,
    connectTimeout: 60000,
    readTimeout: 60000,
    responseType: 'blob',
  });
  return {
    status: response.status,
    data: typeof response.data === 'string' ? response.data : JSON.stringify(response.data),
    headers: (response.headers || {}) as Record<string, string>,
  };
}
