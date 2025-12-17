import { useState, useEffect } from 'react'
import { TrendingUp, Activity, BarChart3, AlertCircle, DollarSign, Bitcoin } from 'lucide-react'
import SymbolCard from './components/SymbolCard'
import './App.css'

const WEBSOCKET_URL = import.meta.env.VITE_WEBSOCKET_URL || 'ws://localhost:8080/ws/market'
const CRYPTO_SYMBOLS = ['BTC', 'ETH', 'BNB', 'SOL', 'XRP']
const DEFAULT_SYMBOLS = CRYPTO_SYMBOLS

const isCrypto = (symbol) => CRYPTO_SYMBOLS.includes(symbol)

function App() {
  const [snapshots, setSnapshots] = useState({})
  const [connectionStatus, setConnectionStatus] = useState({})
  const [selectedSymbol, setSelectedSymbol] = useState(DEFAULT_SYMBOLS[0])

  useEffect(() => {
    const connections = {}

    DEFAULT_SYMBOLS.forEach(symbol => {
      const ws = new WebSocket(`${WEBSOCKET_URL}/${symbol}`)

      ws.onopen = () => {
        console.log(`Connected to ${symbol}`)
        setConnectionStatus(prev => ({ ...prev, [symbol]: 'connected' }))
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.error) {
            console.error(`Error for ${symbol}:`, data.error)
            return
          }
          setSnapshots(prev => ({ ...prev, [symbol]: data }))
        } catch (error) {
          console.error(`Error parsing message for ${symbol}:`, error)
        }
      }

      ws.onerror = (error) => {
        console.error(`WebSocket error for ${symbol}:`, error)
        setConnectionStatus(prev => ({ ...prev, [symbol]: 'error' }))
      }

      ws.onclose = () => {
        console.log(`Disconnected from ${symbol}`)
        setConnectionStatus(prev => ({ ...prev, [symbol]: 'disconnected' }))
        
        // Attempt reconnection after 5 seconds
        setTimeout(() => {
          console.log(`Attempting to reconnect to ${symbol}...`)
          // Trigger re-render to reconnect
        }, 5000)
      }

      connections[symbol] = ws
    })

    return () => {
      Object.values(connections).forEach(ws => ws.close())
    }
  }, [])

  const currentSnapshot = snapshots[selectedSymbol]

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      {/* Header */}
      <header className="bg-slate-800/50 backdrop-blur-sm border-b border-slate-700 sticky top-0 z-50">
        <div className="container mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Bitcoin className="w-8 h-8 text-orange-500" />
              <h1 className="text-2xl font-bold text-white">Finbot Crypto</h1>
              <span className="text-sm text-slate-400">Real-time Cryptocurrency Analysis</span>
            </div>
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <Activity className="w-5 h-5 text-green-500 animate-pulse" />
                <span className="text-sm text-slate-300">Live</span>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-6 py-8">
        {/* Symbol Selector */}
        <div className="mb-8">
          <div className="flex flex-wrap gap-3">
            {DEFAULT_SYMBOLS.map(symbol => {
              const cryptoSymbol = isCrypto(symbol)
              return (
                <button
                  key={symbol}
                  onClick={() => setSelectedSymbol(symbol)}
                  className={`px-6 py-3 rounded-lg font-semibold transition-all duration-200 flex items-center gap-2 ${
                    selectedSymbol === symbol
                      ? 'bg-orange-600 text-white shadow-lg shadow-orange-500/50'
                      : 'bg-slate-700/50 text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  <Bitcoin className="w-4 h-4" />
                  {symbol}
                  {connectionStatus[symbol] === 'connected' && (
                    <span className="inline-block w-2 h-2 bg-green-500 rounded-full"></span>
                  )}
                  {connectionStatus[symbol] === 'error' && (
                    <span className="inline-block w-2 h-2 bg-red-500 rounded-full"></span>
                  )}
                </button>
              )
            })}
          </div>
        </div>

        {/* Symbol Details */}
        {currentSnapshot ? (
          <SymbolCard snapshot={currentSnapshot} />
        ) : (
          <div className="bg-slate-800/50 backdrop-blur-sm rounded-xl p-12 text-center border border-slate-700">
            <AlertCircle className="w-16 h-16 text-slate-500 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-slate-300 mb-2">
              Waiting for data...
            </h3>
            <p className="text-slate-400">
              Connecting to market data stream for {selectedSymbol}
            </p>
          </div>
        )}

        {/* All Symbols Overview */}
        <div className="mt-12">
          <h2 className="text-2xl font-bold text-white mb-6 flex items-center">
            <Bitcoin className="w-6 h-6 mr-2 text-orange-500" />
            Crypto Market Overview
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {DEFAULT_SYMBOLS.map(symbol => {
              const snapshot = snapshots[symbol]
              if (!snapshot) return null

              const priceChange = snapshot.monteCarloResults?.expectedReturn || 0
              const isPositive = priceChange >= 0

              const cryptoSymbol = isCrypto(symbol)
              
              return (
                <div
                  key={symbol}
                  onClick={() => setSelectedSymbol(symbol)}
                  className="bg-slate-800/50 backdrop-blur-sm rounded-lg p-6 border border-slate-700 cursor-pointer transition-all duration-200 hover:border-orange-500 hover:shadow-lg hover:shadow-orange-500/20"
                >
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex items-center gap-2">
                      <Bitcoin className="w-5 h-5 text-orange-500" />
                      <div>
                        <h3 className="text-xl font-bold text-white">{symbol}</h3>
                        <p className="text-sm text-slate-400">Crypto • {snapshot.marketState}</p>
                      </div>
                    </div>
                    <div className={`px-3 py-1 rounded-full text-sm font-semibold ${
                      snapshot.marketState === 'BULLISH' ? 'bg-green-500/20 text-green-400' :
                      snapshot.marketState === 'BEARISH' ? 'bg-red-500/20 text-red-400' :
                      'bg-yellow-500/20 text-yellow-400'
                    }`}>
                      {snapshot.marketState}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Price</span>
                      <span className="text-white font-semibold">
                        ${parseFloat(snapshot.currentPrice).toFixed(2)}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Expected Return</span>
                      <span className={`font-semibold ${isPositive ? 'text-green-400' : 'text-red-400'}`}>
                        {isPositive ? '+' : ''}{(priceChange * 100).toFixed(2)}%
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-slate-400">Volatility</span>
                      <span className="text-white">
                        {(parseFloat(snapshot.bayesianMetrics?.volatility || 0) * 100).toFixed(2)}%
                      </span>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-slate-800/50 backdrop-blur-sm border-t border-slate-700 mt-12">
        <div className="container mx-auto px-6 py-6">
          <p className="text-center text-slate-400 text-sm">
            Finbot Crypto © 2024 - Real-time Cryptocurrency Analysis Platform
          </p>
        </div>
      </footer>
    </div>
  )
}

export default App
