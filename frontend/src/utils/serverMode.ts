const MANUAL_MODE_KEY = 'uqm_manual_srv';

/** 进入「待连接」手工配置模式：显示重新配置按钮，直至登录成功 */
export function enterManualServerMode() {
  sessionStorage.setItem(MANUAL_MODE_KEY, '1');
}

export function exitManualServerMode() {
  sessionStorage.removeItem(MANUAL_MODE_KEY);
}

export function isManualServerMode(): boolean {
  return sessionStorage.getItem(MANUAL_MODE_KEY) === '1';
}
