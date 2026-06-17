/** 批量删除时需输入的确认语，前后端保持一致 */
export const DELETE_CONFIRM_PHRASE = '我确认删除';

export interface BatchDeleteResult {
  deletedCount: number;
  errors?: string[];
}
