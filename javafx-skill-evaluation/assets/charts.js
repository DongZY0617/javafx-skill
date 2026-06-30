(function() {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();
  var danger = style.getPropertyValue('--danger').trim();
  var warn = style.getPropertyValue('--warn').trim();
  var success = style.getPropertyValue('--success').trim();

  // Initialize Mermaid
  if (typeof mermaid !== 'undefined') {
    mermaid.initialize({ startOnLoad: true, theme: 'neutral', securityLevel: 'loose' });
  }

  // Color helper
  function scoreColor(val) {
    if (val >= 9) return success;
    if (val >= 7) return warn;
    return danger;
  }

  // --- Chart 1: Radar Chart (11 skills, 5 dimensions) ---
  var radarEl = document.getElementById('chart-radar');
  if (radarEl) {
    var radar = echarts.init(radarEl, null, { renderer: 'svg' });
    radar.setOption({
      animation: false,
      tooltip: { appendToBody: true },
      legend: {
        data: ['requirements','architect','designer','developer','code-reviewer','runner','tester','refactorer','docgen','deployer','orchestrator'],
        bottom: 0,
        type: 'scroll',
        textStyle: { color: muted, fontSize: 10 },
        pageTextStyle: { color: muted }
      },
      radar: {
        indicator: [
          { name: 'SKILL.md', max: 10 },
          { name: 'EVALUATE', max: 10 },
          { name: 'References', max: 10 },
          { name: 'Schemas', max: 10 },
          { name: 'Integration', max: 10 }
        ],
        center: ['50%', '48%'],
        radius: '62%',
        axisName: { color: ink, fontSize: 12, fontWeight: 700 },
        splitLine: { lineStyle: { color: rule } },
        splitArea: { areaStyle: { color: [bg2, '#f1f5f9'] } },
        axisLine: { lineStyle: { color: rule } }
      },
      series: [{
        type: 'radar',
        data: [
          { value: [8,7,8,7,9], name: 'requirements', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } },
          { value: [9,9,9,7,9], name: 'architect', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } },
          { value: [7,7,8,6,7], name: 'designer', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } },
          { value: [9,9,10,8,10], name: 'developer', lineStyle: { width: 2 }, areaStyle: { opacity: 0.1 } },
          { value: [9,7,8,9,9], name: 'code-reviewer', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } },
          { value: [9,9,7,9,10], name: 'runner', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } },
          { value: [10,10,7,10,10], name: 'tester', lineStyle: { width: 2 }, areaStyle: { opacity: 0.1 } },
          { value: [8,8,8,5,5], name: 'refactorer', lineStyle: { width: 2, color: danger }, areaStyle: { opacity: 0.1, color: danger } },
          { value: [8,9,8,7,7], name: 'docgen', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } },
          { value: [9,9,9,8,7], name: 'deployer', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } },
          { value: [9,7,8,8,9], name: 'orchestrator', lineStyle: { width: 1.5 }, areaStyle: { opacity: 0.05 } }
        ],
        color: [accent, accent2, '#7c3aed', '#0d9488', '#db2777', '#ea580c', success, danger, '#6366f1', '#059669', '#475569']
      }]
    });
    window.addEventListener('resize', function() { radar.resize(); });
  }

  // --- Chart 2: Ranking Bar Chart ---
  var rankEl = document.getElementById('chart-ranking');
  if (rankEl) {
    var ranking = echarts.init(rankEl, null, { renderer: 'svg' });
    var rankData = [
      { name: 'tester', val: 9.4 },
      { name: 'developer', val: 9.2 },
      { name: 'runner', val: 8.8 },
      { name: 'architect', val: 8.6 },
      { name: 'code-reviewer', val: 8.4 },
      { name: 'deployer', val: 8.4 },
      { name: 'orchestrator', val: 8.4 },
      { name: 'requirements', val: 8.0 },
      { name: 'docgen', val: 7.8 },
      { name: 'designer', val: 7.4 },
      { name: 'refactorer', val: 6.8 }
    ];
    ranking.setOption({
      animation: false,
      tooltip: { appendToBody: true, formatter: function(p) { return p.name + ': ' + p.value + ' / 10'; } },
      grid: { left: 120, right: 60, top: 20, bottom: 30 },
      xAxis: {
        type: 'value', max: 10, min: 0,
        axisLine: { lineStyle: { color: rule } },
        axisLabel: { color: muted, fontSize: 11 },
        splitLine: { lineStyle: { color: rule } }
      },
      yAxis: {
        type: 'category',
        data: rankData.map(function(d) { return d.name; }).reverse(),
        axisLine: { lineStyle: { color: rule } },
        axisLabel: { color: ink, fontSize: 12, fontWeight: 700 }
      },
      series: [{
        type: 'bar',
        data: rankData.map(function(d) {
          return { value: d.val, itemStyle: { color: scoreColor(d.val), borderRadius: [0, 4, 4, 0] } };
        }).reverse(),
        barWidth: '55%',
        label: {
          show: true, position: 'right',
          formatter: function(p) { return p.value.toFixed(1); },
          color: ink, fontSize: 12, fontWeight: 700
        }
      }]
    });
    window.addEventListener('resize', function() { ranking.resize(); });
  }

  // --- Chart 3: Loop Completeness ---
  var loopEl = document.getElementById('chart-loop');
  if (loopEl) {
    var loopChart = echarts.init(loopEl, null, { renderer: 'svg' });
    loopChart.setOption({
      animation: false,
      tooltip: { appendToBody: true, trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: {
        data: ['完整', '部分完整', '缺失'],
        bottom: 0,
        textStyle: { color: muted, fontSize: 11 }
      },
      grid: { left: 130, right: 40, top: 20, bottom: 50 },
      xAxis: {
        type: 'value', max: 100,
        axisLine: { lineStyle: { color: rule } },
        axisLabel: { color: muted, fontSize: 11, formatter: '{value}%' },
        splitLine: { lineStyle: { color: rule } }
      },
      yAxis: {
        type: 'category',
        data: ['需求→架构', '架构→设计', '设计→开发', '开发→审查∥验证', '审查∥验证→测试', '测试→重构', '重构→开发', '测试→文档', '文档→部署', '部署→生产反馈'],
        axisLine: { lineStyle: { color: rule } },
        axisLabel: { color: ink, fontSize: 11 }
      },
      series: [
        {
          name: '完整',
          type: 'bar',
          stack: 'total',
          data: [100, 100, 80, 100, 90, 100, 0, 100, 90, 0],
          itemStyle: { color: success },
          barWidth: '50%'
        },
        {
          name: '部分完整',
          type: 'bar',
          stack: 'total',
          data: [0, 0, 20, 0, 10, 0, 50, 0, 10, 0],
          itemStyle: { color: warn }
        },
        {
          name: '缺失',
          type: 'bar',
          stack: 'total',
          data: [0, 0, 0, 0, 0, 0, 50, 0, 0, 100],
          itemStyle: { color: danger }
        }
      ]
    });
    window.addEventListener('resize', function() { loopChart.resize(); });
  }

})();
