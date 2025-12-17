import { useEffect, useRef } from 'react'
import { createChart } from 'lightweight-charts'
import { TrendingUp, TrendingDown, Activity, DollarSign, BarChart2, Target } from 'lucide-react'

export default function SymbolCard({ snapshot }) {
  const chartContainerRef = useRef(null)
  const chartRef = useRef(null)

  useEffect(() => {
    if (!chartContainerRef.current || !snapshot.arimaForecast?.predictions) return

    // Create chart
    const chart = createChart(chartContainerRef.current, {
      width: chartContainerRef.current.clientWidth,
      height: 300,
      layout: {
        background: { color: 'transparent' },
        textColor: '#94a3b8',
      },
      grid: {
        vertLines: { color: '#1e293b' },
        horzLines: { color: '#1e293b' },
      },
      timeScale: {
        borderColor: '#334155',
      },
    })

    const lineSeries = chart.addLineSeries({
      color: '#3b82f6',
      lineWidth: 2,
    })

    // Prepare data for chart
    const predictions = snapshot.arimaForecast.predictions
    const currentTime = Math.floor(Date.now() / 1000)
    const data = predictions.map((price, index) => ({
      time: currentTime + (index * 86400), // Add days
      value: parseFloat(price)
    }))

    lineSeries.setData(data)
    chart.timeScale().fitContent()

    chartRef.current = chart

    // Handle resize
    const handleResize = () => {
      if (chartContainerRef.current && chartRef.current) {
        chartRef.current.applyOptions({
          width: chartContainerRef.current.clientWidth
        })
      }
    }

    window.addEventListener('resize', handleResize)

    return () => {
      window.removeEventListener('resize', handleResize)
      if (chartRef.current) {
        chartRef.current.remove()
      }
    }
  }, [snapshot])

  const { 
    symbol, 
    currentPrice, 
    bayesianMetrics, 
    arimaForecast, 
    monteCarloResults, 
    marketState 
  } = snapshot

  const expectedReturn = monteCarloResults?.expectedReturn || 0
  const isPositive = expectedReturn >= 0

  return (
    <div className="space-y-6">
      {/* Header Card */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-800 rounded-xl p-8 shadow-2xl">
        <div className="flex justify-between items-start">
          <div>
            <h2 className="text-4xl font-bold text-white mb-2">{symbol}</h2>
            <p className="text-blue-100 text-lg">
              ${parseFloat(currentPrice).toFixed(2)}
            </p>
          </div>
          <div className={`px-4 py-2 rounded-full text-lg font-bold ${
            marketState === 'BULLISH' ? 'bg-green-500 text-white' :
            marketState === 'BEARISH' ? 'bg-red-500 text-white' :
            'bg-yellow-500 text-white'
          }`}>
            {marketState}
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* Bayesian Metrics */}
        <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-6 border border-slate-700">
          <div className="flex items-center mb-4">
            <Activity className="w-6 h-6 text-purple-500 mr-2" />
            <h3 className="text-lg font-semibold text-white">Bayesian Analysis</h3>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-slate-400">Drift (μ)</span>
              <span className="text-white font-semibold">
                {(parseFloat(bayesianMetrics?.drift || 0) * 100).toFixed(2)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Volatility (σ)</span>
              <span className="text-white font-semibold">
                {(parseFloat(bayesianMetrics?.volatility || 0) * 100).toFixed(2)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Confidence</span>
              <span className="text-white font-semibold">
                {(parseFloat(bayesianMetrics?.confidence || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Sample Size</span>
              <span className="text-white font-semibold">
                {bayesianMetrics?.sampleSize || 0}
              </span>
            </div>
          </div>
        </div>

        {/* Monte Carlo Results */}
        <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-6 border border-slate-700">
          <div className="flex items-center mb-4">
            <Target className="w-6 h-6 text-orange-500 mr-2" />
            <h3 className="text-lg font-semibold text-white">Monte Carlo</h3>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-slate-400">Expected Return</span>
              <span className={`font-semibold ${isPositive ? 'text-green-400' : 'text-red-400'}`}>
                {isPositive ? '+' : ''}{(expectedReturn * 100).toFixed(2)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Prob. Up</span>
              <span className="text-green-400 font-semibold">
                {(parseFloat(monteCarloResults?.probabilityUp || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Prob. Down</span>
              <span className="text-red-400 font-semibold">
                {(parseFloat(monteCarloResults?.probabilityDown || 0) * 100).toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">VaR (95%)</span>
              <span className="text-white font-semibold">
                ${parseFloat(monteCarloResults?.valueAtRisk95 || 0).toFixed(2)}
              </span>
            </div>
          </div>
        </div>

        {/* ARIMA Forecast */}
        <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-6 border border-slate-700">
          <div className="flex items-center mb-4">
            <BarChart2 className="w-6 h-6 text-blue-500 mr-2" />
            <h3 className="text-lg font-semibold text-white">ARIMA Forecast</h3>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between">
              <span className="text-slate-400">Model</span>
              <span className="text-white font-semibold">
                {arimaForecast?.modelOrder || 'N/A'}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Horizon</span>
              <span className="text-white font-semibold">
                {arimaForecast?.horizon || 0} periods
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Next Prediction</span>
              <span className="text-white font-semibold">
                ${parseFloat(arimaForecast?.predictions?.[0] || 0).toFixed(2)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">AIC</span>
              <span className="text-white font-semibold">
                {parseFloat(arimaForecast?.aic || 0).toFixed(2)}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Chart */}
      <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-6 border border-slate-700">
        <h3 className="text-lg font-semibold text-white mb-4">Price Forecast</h3>
        <div ref={chartContainerRef} className="w-full" />
      </div>

      {/* Risk Metrics */}
      <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-6 border border-slate-700">
        <h3 className="text-lg font-semibold text-white mb-4">Risk Metrics</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">VaR 95%</p>
            <p className="text-white font-bold text-lg">
              ${parseFloat(monteCarloResults?.valueAtRisk95 || 0).toFixed(2)}
            </p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">VaR 99%</p>
            <p className="text-white font-bold text-lg">
              ${parseFloat(monteCarloResults?.valueAtRisk99 || 0).toFixed(2)}
            </p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">CVaR</p>
            <p className="text-white font-bold text-lg">
              ${parseFloat(monteCarloResults?.conditionalVaR || 0).toFixed(2)}
            </p>
          </div>
          <div className="text-center">
            <p className="text-slate-400 text-sm mb-1">Simulations</p>
            <p className="text-white font-bold text-lg">
              {monteCarloResults?.simulations?.toLocaleString() || 0}
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
