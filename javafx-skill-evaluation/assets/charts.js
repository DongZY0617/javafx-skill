(function() {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var accent3 = style.getPropertyValue('--accent3').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();
  var warn = style.getPropertyValue('--warn').trim();
  var danger = style.getPropertyValue('--danger').trim();

  // --- Chart 1: SDLC Coverage Radar ---
  var radarEl = document.getElementById('chart-radar');
  if (radarEl) {
    var chartRadar = echarts.init(radarEl, null, { renderer: 'svg' });
    chartRadar.setOption({
      title: {
        text: 'SDLC 各阶段技能覆盖度（%）',
        left: 'center',
        top: 10,
        textStyle: { color: ink, fontSize: 14, fontWeight: 600 }
      },
      tooltip: {
        trigger: 'item',
        appendToBody: true,
        formatter: function(params) {
          return params.name + ': ' + params.value + '%';
        }
      },
      radar: {
        indicator: [
          { name: '需求分析', max: 100 },
          { name: '设计', max: 100 },
          { name: '实现/编码', max: 100 },
          { name: '代码审查', max: 100 },
          { name: '测试', max: 100 },
          { name: '部署/打包', max: 100 },
          { name: '维护', max: 100 }
        ],
        center: ['50%', '58%'],
        radius: '65%',
        axisName: {
          color: ink,
          fontSize: 12,
          fontWeight: 600
        },
        splitLine: { lineStyle: { color: rule } },
        splitArea: { areaStyle: { color: [bg2, 'transparent'] } },
        axisLine: { lineStyle: { color: rule } }
      },
      series: [{
        type: 'radar',
        data: [{
          value: [60, 75, 95, 95, 75, 90, 50],
          name: '覆盖度',
          areaStyle: { color: accent + '33' },
          lineStyle: { color: accent, width: 2 },
          itemStyle: { color: accent },
          label: {
            show: true,
            formatter: '{c}%',
            color: ink,
            fontSize: 11,
            fontWeight: 600
          }
        }]
      }],
      animation: false
    });
    window.addEventListener('resize', function() { chartRadar.resize(); });
  }

  // --- Chart 2: Optimization Priority vs Complexity Matrix ---
  var matrixEl = document.getElementById('chart-matrix');
  if (matrixEl) {
    var chartMatrix = echarts.init(matrixEl, null, { renderer: 'svg' });
    var matrixData = [
      { name: '3.1 审核验证并行', value: [2, 3, 25], priority: 'P0' },
      { name: '3.2 JSON 报告输出', value: [1, 3, 20], priority: 'P0' },
      { name: '3.3 增量编译', value: [1, 3, 22], priority: 'P0' },
      { name: '3.4 需求追踪矩阵', value: [2, 2, 30], priority: 'P1' },
      { name: '3.5 修复回滚', value: [2, 2, 28], priority: 'P1' },
      { name: '3.6 循环次数动态', value: [1, 2, 35], priority: 'P1' },
      { name: '3.7 AST 锚点', value: [3, 2, 32], priority: 'P1' },
      { name: '3.8 可视化仪表盘', value: [3, 1, 40], priority: 'P2' },
      { name: '3.9 并发修复', value: [3, 1, 38], priority: 'P2' },
      { name: '3.10 协议去重', value: [2, 1, 42], priority: 'P2' }
    ];

    var p0Data = matrixData.filter(function(d) { return d.priority === 'P0'; });
    var p1Data = matrixData.filter(function(d) { return d.priority === 'P1'; });
    var p2Data = matrixData.filter(function(d) { return d.priority === 'P2'; });

    function buildSeries(data, color) {
      return {
        type: 'scatter',
        data: data.map(function(d) {
          return {
            value: d.value,
            name: d.name
          };
        }),
        symbolSize: function(data) { return data[2]; },
        itemStyle: {
          color: color,
          opacity: 0.75,
          borderColor: color,
          borderWidth: 2
        },
        emphasis: {
          itemStyle: { opacity: 1, shadowBlur: 10, shadowColor: color }
        },
        label: {
          show: true,
          formatter: function(params) { return params.name; },
          position: 'top',
          color: ink,
          fontSize: 10,
          fontWeight: 600,
          distance: 8
        }
      };
    }

    chartMatrix.setOption({
      title: {
        text: '优化建议：优先级 × 实施复杂度',
        left: 'center',
        top: 10,
        textStyle: { color: ink, fontSize: 14, fontWeight: 600 }
      },
      tooltip: {
        trigger: 'item',
        appendToBody: true,
        formatter: function(params) {
          var p = ['低', '中', '高'][params.value[0] - 1];
          var v = ['P2', 'P1', 'P0'][params.value[1] - 1];
          return params.name + '<br/>复杂度: ' + p + '<br/>优先级: ' + v;
        }
      },
      legend: {
        data: ['P0 立即', 'P1 短期', 'P2 中期'],
        top: 40,
        left: 'center',
        textStyle: { color: muted, fontSize: 11 }
      },
      grid: {
        left: 80,
        right: 60,
        top: 80,
        bottom: 60
      },
      xAxis: {
        type: 'value',
        name: '实施复杂度',
        nameLocation: 'middle',
        nameGap: 35,
        nameTextStyle: { color: muted, fontSize: 12 },
        min: 0.5,
        max: 3.5,
        interval: 1,
        axisLabel: {
          color: muted,
          formatter: function(val) {
            var labels = { 1: '低', 2: '中', 3: '高' };
            return labels[val] || '';
          }
        },
        splitLine: { lineStyle: { color: rule, type: 'dashed' } },
        axisLine: { lineStyle: { color: rule } }
      },
      yAxis: {
        type: 'value',
        name: '优先级',
        nameLocation: 'middle',
        nameGap: 50,
        nameTextStyle: { color: muted, fontSize: 12 },
        min: 0.5,
        max: 3.5,
        interval: 1,
        axisLabel: {
          color: muted,
          formatter: function(val) {
            var labels = { 1: 'P2', 2: 'P1', 3: 'P0' };
            return labels[val] || '';
          }
        },
        splitLine: { lineStyle: { color: rule, type: 'dashed' } },
        axisLine: { lineStyle: { color: rule } }
      },
      series: [
        Object.assign(buildSeries(p0Data, danger), { name: 'P0 立即' }),
        Object.assign(buildSeries(p1Data, warn), { name: 'P1 短期' }),
        Object.assign(buildSeries(p2Data, accent), { name: 'P2 中期' })
      ],
      animation: false
    });
    window.addEventListener('resize', function() { chartMatrix.resize(); });
  }

  // --- Chart 3: Maturity Score Bar Chart ---
  var barEl = document.getElementById('chart-bar');
  if (barEl) {
    var chartBar = echarts.init(barEl, null, { renderer: 'svg' });
    chartBar.setOption({
      title: {
        text: '各维度闭环成熟度评分（当前 vs 目标）',
        left: 'center',
        top: 10,
        textStyle: { color: ink, fontSize: 14, fontWeight: 600 }
      },
      tooltip: {
        trigger: 'axis',
        appendToBody: true,
        axisPointer: { type: 'shadow' }
      },
      legend: {
        data: ['当前评分', '目标评分'],
        top: 40,
        left: 'center',
        textStyle: { color: muted, fontSize: 11 }
      },
      grid: {
        left: 60,
        right: 40,
        top: 80,
        bottom: 70
      },
      xAxis: {
        type: 'category',
        data: ['需求管理', '架构设计', '代码生成', '静态审核', '动态验证', '打包部署', '维护运维'],
        axisLabel: {
          color: muted,
          fontSize: 11,
          rotate: 20
        },
        axisLine: { lineStyle: { color: rule } },
        axisTick: { lineStyle: { color: rule } }
      },
      yAxis: {
        type: 'value',
        name: '成熟度评分',
        nameTextStyle: { color: muted, fontSize: 12 },
        min: 0,
        max: 100,
        interval: 20,
        axisLabel: { color: muted, formatter: '{value}' },
        splitLine: { lineStyle: { color: rule } },
        axisLine: { lineStyle: { color: rule } }
      },
      series: [
        {
          name: '当前评分',
          type: 'bar',
          data: [45, 70, 90, 92, 78, 88, 50],
          itemStyle: {
            color: accent,
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '30%',
          label: {
            show: true,
            position: 'top',
            color: ink,
            fontSize: 10,
            fontWeight: 600
          }
        },
        {
          name: '目标评分',
          type: 'bar',
          data: [80, 85, 95, 95, 90, 92, 85],
          itemStyle: {
            color: accent3,
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '30%',
          label: {
            show: true,
            position: 'top',
            color: ink,
            fontSize: 10,
            fontWeight: 600
          }
        }
      ],
      animation: false
    });
    window.addEventListener('resize', function() { chartBar.resize(); });
  }
})();
