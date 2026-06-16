export interface HelpSection {
  title: string;
  items: string[];
}

const COMMON: HelpSection[] = [
  {
    title: '登录与身份切换',
    items: [
      '使用学校分配的账号密码登录系统。',
      '若您属于多个用户组，点击右上角「身份切换」选择当前工作身份；不同身份看到的菜单和待办任务不同。',
      '切换身份后，「我的任务」只显示当前身份组下分配给您的任务实例。',
    ],
  },
  {
    title: '消息中心',
    items: [
      '顶部铃铛图标显示未读消息数量。',
      '可查看系统通知、组内广播和任务讨论消息，支持标记已读。',
    ],
  },
];

const BY_GROUP: Record<number, HelpSection[]> = {
  1: [
    {
      title: '系统管理员',
      items: [
        '拥有全部功能权限，可在「系统配置」中管理统一认证、邮件/企微通知、数据保留策略和文件存储。',
        '可在「用户管理」「组管理」中维护账号、权限与组织架构。',
        '可创建/发布/分配任务，查看全校统计与操作日志。',
      ],
    },
  ],
  2: [
    {
      title: '校级管理',
      items: [
        '可创建、编辑、发布质量监控任务，并使用任务模板快速配置流程。',
        '可将任务分配给各学院或指定用户组，查看全校进度统计并导出数据。',
        '可在「消息中心」向各组发送广播通知。',
        '可在「用户管理」中维护全校用户（不含组权限配置）。',
      ],
    },
  ],
  3: [
    {
      title: '院级管理',
      items: [
        '数据范围限定为本院：用户管理、任务分配与统计均只涉及本院师生。',
        '可创建本院质量监控任务、分配任务、查看本院统计并导出。',
        '可向相关组发送消息通知。',
      ],
    },
  ],
  4: [
    {
      title: '专家 / 评审',
      items: [
        '请切换到「专家/评审组」身份，在「我的任务」中处理待评分任务。',
        '评分节点支持百分制或等级制（优/良/中/差），需填写评语后提交。',
        '若任务流程中当前节点由其他组负责（如材料提交），您会看到「等待其他组处理」，无需操作。',
        '可在任务详情底部参与「任务讨论」。',
      ],
    },
  ],
  5: [
    {
      title: '材料提交',
      items: [
        '请切换到「材料提交组」身份，在「我的任务」中处理待提交任务。',
        '在提交节点上传材料、填写说明，可先保存草稿再正式提交。',
        '提交完成后，后续「专家评分」「审核」等节点由对应执行组处理，您无需评分或审核。',
        '若看到「等待其他组处理」，表示您的环节已完成，请等待专家或管理员推进流程。',
        '可在任务详情底部参与「任务讨论」。',
      ],
    },
  ],
  6: [
    {
      title: '查看组',
      items: [
        '只读权限：可查看消息与分配给自己的查看类任务节点。',
        '无法创建任务、提交材料或评分。',
      ],
    },
  ],
  7: [
    {
      title: '评分组',
      items: [
        '与专家/评审组类似，在「我的任务」中完成评分节点。',
        '请确认已切换到「评分组」身份后再处理待办。',
      ],
    },
  ],
};

const PERMISSION_HINTS: Record<string, string> = {
  'task:create': '任务管理：创建、编辑草稿任务，配置流程节点与时间范围。',
  'task:config': '任务模板：保存和复用标准流程配置。',
  'task:allocate': '任务分配：将已发布任务分配给学院或用户。',
  'stat:view_all': '统计：查看全校任务进度、评分汇总与导出。',
  'stat:view_college': '统计：查看本院任务进度与导出。',
  'message:send': '消息：向指定用户组发送广播消息。',
  'data:export': '导出：下载 Excel 格式的统计与评语数据。',
  'user:manage': '用户管理：维护账号、所属组与联系方式。',
  'group:manage': '组管理：维护用户组及其权限配置。',
  'system:config': '系统配置：认证、通知、存储与数据保留策略。',
};

export function buildHelpSections(
  currentGroupId: number | null,
  currentGroupName: string | undefined,
  permissions: string[]
): HelpSection[] {
  const sections: HelpSection[] = [
    {
      title: `当前身份：${currentGroupName || '未选择'}`,
      items: [
        `您当前以「${currentGroupName || '未知组'}」身份使用系统。`,
        '不同身份对应不同菜单与待办；处理任务前请确认身份是否正确。',
      ],
    },
    ...COMMON,
  ];

  if (currentGroupId && BY_GROUP[currentGroupId]) {
    sections.push(...BY_GROUP[currentGroupId]);
  }

  const permItems = permissions
    .map((p) => PERMISSION_HINTS[p])
    .filter(Boolean) as string[];

  if (permItems.length > 0) {
    sections.push({
      title: '您拥有的扩展功能',
      items: permItems,
    });
  }

  sections.push({
    title: '常见问题',
    items: [
      '为什么我看到「专家评分」但无法操作？—— 该节点由专家/评审组执行；材料提交组只需完成上传环节，提交后等待即可。',
      '任务整体开始/结束时间由任务创建者在「任务管理」中通过日历设置，用于截止提醒。',
      '遇到权限或流程问题，请联系本院或校级管理员。',
    ],
  });

  return sections;
}
