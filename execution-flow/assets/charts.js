(function() {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var accent3 = style.getPropertyValue('--accent3').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();
  var danger = style.getPropertyValue('--danger').trim();
  var warn = style.getPropertyValue('--warn').trim();

  // --- Gantt Chart ---
  var ganttEl = document.getElementById('chart-gantt');
  if (ganttEl) {
    var chartGantt = echarts.init(ganttEl, null, { renderer: 'svg' });

    // 周为单位的任务数据 [任务名, 开始周, 持续周, 阶段]
    var tasks = [
      ['T1.1 审核验证并行化', 0, 1.5, '阶段一'],
      ['T1.2 JSON 报告输出', 0.5, 1, '阶段一'],
      ['T1.3 增量编译', 1.5, 0.5, '阶段一'],
      ['T2.1 javafx-tester', 2, 2.5, '阶段二'],
      ['T2.2 javafx-docgen', 2.5, 1.5, '阶段二'],
      ['T2.3 需求追踪矩阵', 2, 1, '阶段二'],
      ['T2.4 修复回滚', 3, 1, '阶段二'],
      ['T3.1 javafx-designer', 4, 3.5, '阶段三'],
      ['T3.2 javafx-deployer', 4.5, 2.5, '阶段三'],
      ['T3.3 可视化仪表盘', 5, 1.5, '阶段三'],
      ['T3.4 AST 锚点', 5.5, 2.5, '阶段三'],
      ['T4.1 javafx-architect', 8, 3.5, '阶段四'],
      ['T4.2 javafx-refactorer', 8.5, 3.5, '阶段四'],
      ['T4.3 协议去重', 9, 1.5, '阶段四'],
      ['T4.4 并发修复', 10.5, 2.5, '阶段四']
    ];

    var categories = tasks.map(function(t) { return t[0]; }).reverse();
    var phaseColors = {
      '阶段一': danger,
      '阶段二': warn,
      '阶段三': accent,
      '阶段四': accent2
    };

    var seriesData = tasks.map(function(t, i) {
      var idx = categories.indexOf(t[0]);
      return {
        name: t[3],
        value: [idx, t[1], t[1] + t[2], t[3]],
        itemStyle: { color: phaseColors[t[3]] }
      };
    });

    chartGantt.setOption({
      title: {
        text: '任务甘特图（周）',
        left: 'center',
        top: 10,
        textStyle: { color: ink, fontSize: 14, fontWeight: 600 }
      },
      tooltip: {
        trigger: 'item',
        appendToBody: true,
        formatter: function(params) {
          var v = params.value;
          return params.name + '<br/>起始：第 ' + (v[1] + 1).toFixed(1) + ' 周<br/>结束：第 ' + (v[2] + 1).toFixed(1) + ' 周<br/>持续：' + (v[2] - v[1]).toFixed(1) + ' 周';
        }
      },
      legend: {
        data: ['阶段一', '阶段二', '阶段三', '阶段四'],
        top: 40,
        left: 'center',
        textStyle: { color: muted, fontSize: 11 }
      },
      grid: {
        left: 180,
        right: 50,
        top: 80,
        bottom: 50
      },
      xAxis: {
        type: 'value',
        name: '周',
        nameLocation: 'middle',
        nameGap: 30,
        nameTextStyle: { color: muted, fontSize: 12 },
        min: 0,
        max: 14,
        interval: 1,
        axisLabel: {
          color: muted,
          formatter: function(val) { return 'W' + (val + 1); }
        },
        splitLine: { lineStyle: { color: rule, type: 'dashed' } },
        axisLine: { lineStyle: { color: rule } }
      },
      yAxis: {
        type: 'category',
        data: categories,
        axisLabel: { color: ink, fontSize: 10 },
        axisLine: { lineStyle: { color: rule } },
        axisTick: { lineStyle: { color: rule } }
      },
      series: [{
        type: 'custom',
        renderItem: function(params, api) {
          var categoryIndex = api.value(0);
          var start = api.coord([api.value(1), categoryIndex]);
          var end = api.coord([api.value(2), categoryIndex]);
          var height = api.size([0, 1])[1] * 0.6;
          var itemStyle = api.visual('itemStyle');
          return {
            type: 'rect',
            shape: {
              x: start[0],
              y: start[1] - height / 2,
              width: end[0] - start[0],
              height: height
            },
            style: api.style()
          };
        },
        data: seriesData,
        encode: { x: [1, 2], y: 0 }
      }],
      animation: false
    });
    window.addEventListener('resize', function() { chartGantt.resize(); });
  }
})();
