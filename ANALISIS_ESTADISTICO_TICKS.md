# Análisis Estadístico del Procesamiento de Ticks

## Resumen Ejecutivo

Este documento describe en detalle el pipeline de análisis estadístico aplicado al procesamiento de ticks de mercado en tiempo real. El sistema implementa un enfoque integrado de tres etapas denominado **ABC (ARIMA-Bayes-Carlo)**, que combina métodos de series temporales, inferencia bayesiana y simulación estocástica para generar predicciones y métricas de riesgo.

---

## 1. Arquitectura del Sistema de Procesamiento

### 1.1 Flujo de Datos

```
Market Ticks (Stream) → Ventana Deslizante → ABC Analysis → Market Snapshot
                         (30-500 ticks)      (3 etapas)     (Persistencia)
```

### 1.2 Estructura de un Tick

Cada tick de mercado contiene:

- **symbol**: Identificador del activo
- **price**: Precio de transacción (BigDecimal)
- **volume**: Volumen negociado
- **timestamp**: Marca temporal (Instant)
- **bid/ask**: Precios de oferta/demanda
- **high/low/open**: Precios OHLC

### 1.3 Ventana Deslizante

El sistema mantiene una ventana deslizante por símbolo con las siguientes características:

- **Tamaño mínimo**: 30 ticks (requerido para análisis)
- **Tamaño máximo**: 500 ticks (límite de memoria)
- **Política de eliminación**: FIFO (First-In-First-Out)
- **Frecuencia de snapshot**: Cada 5 segundos (configurable)

---

## 2. Pipeline ABC: Análisis Integrado de Tres Etapas

El análisis ABC es un pipeline secuencial donde cada etapa informa a la siguiente, creando un sistema de retroalimentación estadística.

### 2.1 Etapa 1: Análisis ARIMA con Detección de Quiebres Estructurales

#### 2.1.1 Objetivo

Detectar tendencias en los patrones de precios e identificar cambios estructurales que invaliden modelos históricos.

#### 2.1.2 Metodología

**Suavizado Exponencial de Holt (Holt's Exponential Smoothing)**

El sistema utiliza el método de Holt para descomponer la serie temporal en nivel y tendencia:

```
L_t = α·P_t + (1-α)·(L_{t-1} + T_{t-1})
T_t = β·(L_t - L_{t-1}) + (1-β)·T_{t-1}
```

Donde:
- `L_t`: Nivel en el tiempo t
- `T_t`: Tendencia en el tiempo t
- `P_t`: Precio observado en el tiempo t
- `α = 0.3`: Parámetro de suavizado del nivel
- `β = 0.1`: Parámetro de suavizado de la tendencia

**Inicialización:**
- `L_0 = P_0` (primer precio)
- `T_0 = (P_n - P_0) / n` (tendencia lineal inicial)

#### 2.1.3 Detección de Quiebres Estructurales (CUSUM)

Se implementa el algoritmo **CUSUM (Cumulative Sum Control Chart)** para detectar cambios abruptos en la distribución de precios.

**Estadístico CUSUM:**

```
S_i = Σ_{j=k}^{i} [(P_j - μ) / σ]
```

Donde:
- `k = 0.7·n`: Se monitorea el último 30% de observaciones
- `μ`: Media histórica de precios
- `σ`: Desviación estándar histórica
- `S_i`: Suma acumulativa de desviaciones estandarizadas

**Criterio de Detección:**

```
Quiebre Estructural ⟺ max|S_i| > 3·σ
```

Un quiebre estructural indica:
- Cambio de régimen de mercado
- Invalidación de parámetros históricos
- Necesidad de recalibración del modelo

#### 2.1.4 Cálculo de Confianza ARIMA

```
C_ARIMA = (1 - 1/√(n+1)) · P_break

donde:
P_break = 0.7 si hay quiebre estructural
P_break = 1.0 en caso contrario
```

La confianza decrece con:
- Menor tamaño muestral
- Presencia de quiebres estructurales (penalización del 30%)

#### 2.1.5 Salidas de la Etapa 1

- **Tendencia (T)**: Cambio promedio por tick
- **Tendencia Porcentual**: `(T / μ) × 100`
- **Quiebre Estructural**: Booleano
- **Estadístico CUSUM**: Valor del estadístico
- **Umbral**: `3·σ`
- **Confianza**: `C_ARIMA ∈ [0, 1]`

---

### 2.2 Etapa 2: Análisis Bayesiano de Momentum con Prior Informado

#### 2.2.1 Objetivo

Estimar el drift (momentum) y la volatilidad del activo utilizando inferencia bayesiana, incorporando la información de tendencia detectada en la Etapa 1 como prior informativo.

#### 2.2.2 Transformación de Datos: Log-Returns

Se calculan los **retornos logarítmicos** para obtener propiedades estadísticas deseables:

```
r_t = ln(P_t / P_{t-1})
```

**Ventajas de log-returns:**
- Simetría temporal: `r_{t→t+k} = -r_{t+k→t}`
- Aditividad temporal: `r_{0→t} = Σ r_i`
- Aproximación normal para pequeños cambios
- Estacionariedad mejorada

#### 2.2.3 Inferencia Bayesiana con Conjugate Prior

Se asume que los retornos siguen una distribución normal:

```
r_t ~ N(μ, σ²)
```

**Prior (Distribución a priori):**

El prior se construye utilizando la señal ARIMA:

```
μ_prior = T_ARIMA × 10.0
σ²_prior = 0.01 × (2 - C_ARIMA)
n_prior = 1 + C_ARIMA
```

Donde:
- `T_ARIMA`: Tendencia detectada en Etapa 1
- `C_ARIMA`: Confianza ARIMA
- El factor 10.0 escala la tendencia al espacio de retornos
- Mayor confianza ARIMA → menor varianza prior → prior más informativo

**Likelihood (Verosimilitud):**

```
μ_sample = (1/n) Σ r_i
σ²_sample = (1/n) Σ (r_i - μ_sample)²
n_sample = número de retornos
```

**Posterior (Distribución a posteriori):**

Utilizando conjugate priors para la distribución normal:

```
n_posterior = n_prior + n_sample

μ_posterior = (n_prior·μ_prior + n_sample·μ_sample) / n_posterior

σ²_posterior = [n_prior·σ²_prior + n_sample·σ²_sample + 
                (n_prior·n_sample/n_posterior)·(μ_sample - μ_prior)²] / n_posterior
```

El término adicional en la varianza posterior captura la **incertidumbre epistémica** debido a la discrepancia entre prior y datos.

#### 2.2.4 Anualización de Parámetros

Asumiendo datos diarios, se anualizan los parámetros:

```
Drift Anualizado = μ_posterior × 252
Volatilidad Anualizada = √(σ²_posterior × 252)
```

Donde 252 es el número de días de trading por año.

#### 2.2.5 Ajuste de Confianza

```
C_Bayes = (1 - 1/√(n_sample + 1)) · P_break

donde:
P_break = 0.7 si hay quiebre estructural
P_break = 1.0 en caso contrario
```

La confianza bayesiana se penaliza si se detectó un quiebre estructural en la Etapa 1.

#### 2.2.6 Salidas de la Etapa 2

- **Drift (μ)**: Retorno esperado anualizado
- **Volatilidad (σ)**: Desviación estándar anualizada
- **Confianza**: `C_Bayes ∈ [0, 1]`
- **Prior Mean**: Media a priori
- **Posterior Mean**: Media a posteriori
- **Prior Variance**: Varianza a priori
- **Posterior Variance**: Varianza a posteriori

---

### 2.3 Etapa 3: Simulación Monte Carlo con Movimiento Browniano Geométrico

#### 2.3.1 Objetivo

Simular miles de trayectorias de precios futuras para estimar distribuciones de probabilidad y métricas de riesgo.

#### 2.3.2 Modelo Estocástico: Geometric Brownian Motion (GBM)

El precio del activo se modela como:

```
dS_t = μ·S_t·dt + σ·S_t·dW_t
```

Donde:
- `S_t`: Precio en el tiempo t
- `μ`: Drift (de la Etapa 2)
- `σ`: Volatilidad (de la Etapa 2)
- `dW_t`: Proceso de Wiener (Brownian motion)

**Solución discreta (Esquema de Euler-Maruyama):**

```
S_{t+Δt} = S_t · exp[(μ - 0.5·σ²)·Δt + σ·√Δt·Z]

donde:
Z ~ N(0, 1)
Δt = 1/252 (paso diario)
```

#### 2.3.3 Parámetros de Simulación

- **Número de simulaciones**: 10,000 (configurable)
- **Horizonte temporal**: 7 días (configurable)
- **Paso temporal**: Δt = 1/252 (diario)

#### 2.3.4 Algoritmo de Simulación

```
Para cada simulación i = 1, ..., N:
    S = S_0 (precio actual)
    Para cada día t = 1, ..., H:
        Z ~ N(0, 1)
        dW = Z · √Δt
        S = S · exp[(μ - 0.5·σ²)·Δt + σ·dW]
    Almacenar S_final[i] = S
```

#### 2.3.5 Métricas Calculadas

**Probabilidades Direccionales:**

```
P(↑) = #{S_final > S_0} / N
P(↓) = #{S_final < S_0} / N
P(→) = 1 - P(↑) - P(↓)
```

**Retorno Esperado:**

```
E[R] = (E[S_final] - S_0) / S_0

donde:
E[S_final] = (1/N) Σ S_final[i]
```

**Value at Risk (VaR):**

VaR mide la pérdida máxima esperada con un nivel de confianza dado:

```
VaR_α = S_0 - Percentil_α(S_final)

VaR_95 = S_0 - Percentil_5(S_final)
VaR_99 = S_0 - Percentil_1(S_final)
```

Interpretación: "Con 95% de confianza, la pérdida no excederá VaR_95"

**Conditional VaR (CVaR / Expected Shortfall):**

CVaR mide la pérdida esperada en el peor α% de casos:

```
CVaR_α = E[S_0 - S_final | S_final ≤ Percentil_α(S_final)]

CVaR_5% = (1/k) Σ_{i=1}^{k} (S_0 - S_final[i])

donde k = 0.05·N (peor 5% de simulaciones)
```

CVaR es una **medida de riesgo coherente** (satisface subaditividad, monotonicidad, homogeneidad positiva e invarianza traslacional).

**Percentiles de Distribución:**

Se calculan percentiles 5, 25, 50, 75, 95 de la distribución de precios finales para construir intervalos de confianza.

#### 2.3.6 Salidas de la Etapa 3

- **Probabilidad Alza**: P(↑)
- **Probabilidad Baja**: P(↓)
- **Probabilidad Neutral**: P(→)
- **Retorno Esperado**: E[R]
- **Cambio de Precio Esperado**: E[ΔS]
- **VaR 95%**: Pérdida máxima al 95% de confianza
- **VaR 99%**: Pérdida máxima al 99% de confianza
- **CVaR**: Expected Shortfall
- **Percentiles**: Distribución de precios objetivo
- **Escenario Más Probable**: UPWARD/DOWNWARD/SIDEWAYS

---

## 3. Integración ABC: Confianza y Régimen de Mercado

### 3.1 Confianza Integrada

La confianza del análisis ABC se calcula como la **media geométrica** de las confianzas individuales, ajustada por estabilidad:

```
C_ABC = √(C_ARIMA · C_Bayes) · F_stability

donde:
F_stability = 0.7 si hay quiebre estructural
F_stability = 1.0 en caso contrario
```

La media geométrica penaliza más severamente confianzas bajas en cualquier etapa, reflejando que el sistema es tan fuerte como su eslabón más débil.

### 3.2 Determinación de Régimen de Mercado

El sistema clasifica el mercado en 8 regímenes posibles:

#### 3.2.1 Regímenes Especiales

- **REGIME_CHANGE**: Quiebre estructural detectado → Recalibración necesaria
- **HIGH_VOLATILITY**: σ > 0.50 → Mercado extremadamente volátil

#### 3.2.2 Regímenes Normales

Se evalúan tres señales alcistas:

```
Señal 1: Tendencia% > 2.0
Señal 2: Drift > 0.05
Señal 3: P(↑) > 0.6
```

**Clasificación:**

- **BULLISH_STABLE**: ≥2 señales alcistas, σ ≤ 0.30
- **BULLISH_VOLATILE**: ≥2 señales alcistas, σ > 0.30
- **BEARISH_STABLE**: ≥2 señales bajistas, σ ≤ 0.30
- **BEARISH_VOLATILE**: ≥2 señales bajistas, σ > 0.30
- **NEUTRAL_STABLE**: Señales mixtas, σ ≤ 0.30
- **NEUTRAL_VOLATILE**: Señales mixtas, σ > 0.30

### 3.3 Trigger de Recalibración

El sistema señala necesidad de recalibración cuando:

```
Recalibración ⟺ Quiebre_Estructural ∨ (σ > 0.50)
```

Esto indica que los parámetros históricos ya no son válidos y el modelo debe reiniciarse.

---

## 4. Propiedades Estadísticas del Sistema

### 4.1 Supuestos del Modelo

1. **Log-returns son i.i.d.** (independientes e idénticamente distribuidos)
2. **Normalidad de retornos** (aproximación válida para horizontes cortos)
3. **No arbitraje** (implícito en GBM)
4. **Mercados eficientes** (precios reflejan toda la información disponible)
5. **Volatilidad constante** en el horizonte de predicción

### 4.2 Limitaciones Conocidas

1. **Fat tails**: Los mercados reales exhiben colas más pesadas que la distribución normal
2. **Volatility clustering**: La volatilidad tiende a agruparse en el tiempo (no capturado por GBM simple)
3. **Jumps**: Eventos extremos no son modelados por difusión continua
4. **Microstructure noise**: Ticks de alta frecuencia contienen ruido de microestructura
5. **Asimetría**: Los mercados pueden exhibir skewness (asimetría) no capturada

### 4.3 Robustez del Sistema

**Manejo de Outliers:**
- Detección de quiebres estructurales (CUSUM)
- Penalización de confianza en presencia de inestabilidad
- Ventana deslizante limita impacto de datos antiguos

**Convergencia:**
- Bayesian updating converge a la verdadera distribución con n → ∞
- Monte Carlo converge por Ley de Grandes Números
- Error estándar de MC: O(1/√N)

**Adaptabilidad:**
- Prior bayesiano se actualiza con nueva información
- Ventana deslizante permite adaptación a cambios de régimen
- Detección automática de quiebres estructurales

---

## 5. Interpretación de Resultados

### 5.1 Snapshot de Mercado

Cada snapshot contiene:

```json
{
  "symbol": "AAPL",
  "timestamp": "2026-04-12T23:53:00Z",
  "currentPrice": 150.25,
  "marketState": "BULLISH_STABLE",
  "abcAnalysis": {
    "arimaSignal": {
      "trend": 0.0523,
      "trendPercentage": 2.35,
      "structuralBreakDetected": false,
      "confidence": 0.8912,
      "description": "Price increasing 2.35% in trend"
    },
    "momentumMetrics": {
      "drift": 0.0847,
      "volatility": 0.2134,
      "confidence": 0.9123,
      "priorMean": 0.0523,
      "posteriorMean": 0.0336
    },
    "marketPrediction": {
      "probabilityUp": 0.6234,
      "probabilityDown": 0.3766,
      "expectedPriceChange": 1.23,
      "expectedPriceChangePercent": 0.82,
      "mostLikelyScenario": "UPWARD_MOVEMENT",
      "priceTargets": [
        {"percentile": 5, "price": 145.12, "changePercent": -3.42},
        {"percentile": 50, "price": 151.48, "changePercent": 0.82},
        {"percentile": 95, "price": 157.89, "changePercent": 5.09}
      ]
    },
    "abcIntegrationConfidence": 0.9012,
    "needsRecalibration": false,
    "marketRegime": "BULLISH_STABLE"
  }
}
```

### 5.2 Guía de Interpretación

**Alta Confianza (C_ABC > 0.8):**
- Señales consistentes entre las tres etapas
- Datos suficientes y estables
- Predicciones confiables

**Confianza Media (0.5 < C_ABC ≤ 0.8):**
- Señales parcialmente consistentes
- Posible volatilidad moderada
- Usar con precaución

**Baja Confianza (C_ABC ≤ 0.5):**
- Señales inconsistentes o datos insuficientes
- Alta incertidumbre
- No recomendable para decisiones críticas

**Quiebre Estructural:**
- Invalidación de modelos históricos
- Esperar estabilización antes de actuar
- Recalibración automática en curso

---

## 6. Consideraciones Computacionales

### 6.1 Complejidad Temporal

- **Procesamiento de tick**: O(1) - inserción en ventana
- **Análisis ARIMA**: O(n) - n = tamaño de ventana
- **Análisis Bayesiano**: O(n) - cálculo de estadísticos
- **Monte Carlo**: O(N·H) - N simulaciones, H horizonte
- **Total por snapshot**: O(n + N·H) ≈ O(10,000·7) ≈ O(70,000)

### 6.2 Paralelización

El sistema utiliza **Virtual Threads (Java 21)** para ejecutar análisis en paralelo:

```
Bayesian Analysis ──┐
                    ├──> Combine Results
ARIMA Forecast   ──┤
                    │
Monte Carlo      ──┘
```

Esto reduce la latencia de generación de snapshots significativamente.

### 6.3 Memoria

- **Por símbolo**: ~500 ticks × 200 bytes ≈ 100 KB
- **Simulaciones MC**: 10,000 × 8 bytes ≈ 80 KB (temporal)
- **Total por símbolo**: ~200 KB
- **Para 100 símbolos**: ~20 MB

---

## 7. Validación y Backtesting

### 7.1 Métricas de Validación Recomendadas

1. **Calibración de Probabilidades**: Reliability diagrams
2. **Sharpness**: Ancho de intervalos de confianza
3. **Coverage**: Porcentaje de realizaciones dentro de intervalos
4. **Brier Score**: Para predicciones probabilísticas
5. **CRPS (Continuous Ranked Probability Score)**: Para distribuciones completas

### 7.2 Tests Estadísticos

1. **Ljung-Box Test**: Autocorrelación de residuos
2. **Jarque-Bera Test**: Normalidad de retornos
3. **ARCH Test**: Heterocedasticidad condicional
4. **Kupiec Test**: Backtesting de VaR

---

## 8. Referencias Teóricas

### 8.1 Series Temporales
- Holt, C. C. (1957). "Forecasting seasonals and trends by exponentially weighted moving averages"
- Page, E. S. (1954). "Continuous Inspection Schemes"

### 8.2 Inferencia Bayesiana
- Gelman, A. et al. (2013). "Bayesian Data Analysis"
- Murphy, K. P. (2012). "Machine Learning: A Probabilistic Perspective"

### 8.3 Finanzas Cuantitativas
- Black, F., & Scholes, M. (1973). "The Pricing of Options and Corporate Liabilities"
- Hull, J. C. (2018). "Options, Futures, and Other Derivatives"
- Glasserman, P. (2003). "Monte Carlo Methods in Financial Engineering"

### 8.4 Gestión de Riesgo
- Artzner, P. et al. (1999). "Coherent Measures of Risk"
- Rockafellar, R. T., & Uryasev, S. (2000). "Optimization of Conditional Value-at-Risk"

---

## 9. Configuración y Parámetros

### 9.1 Parámetros Configurables

```properties
# Ventana de datos
analytics.window.min=30
analytics.window.max=500

# Frecuencia de snapshots
analytics.snapshot.interval=5s

# ARIMA
arima.horizon.periods=7
arima.alpha=0.3
arima.beta=0.1

# Monte Carlo
monte.carlo.simulations=10000
monte.carlo.horizon.days=7

# Umbrales
analytics.volatility.high.threshold=0.50
analytics.cusum.threshold.multiplier=3.0
analytics.structural.break.penalty=0.7
```

### 9.2 Recomendaciones de Tuning

- **Alta frecuencia (segundos)**: Reducir ventana a 50-100 ticks
- **Baja frecuencia (minutos)**: Aumentar ventana a 200-500 ticks
- **Mercados volátiles**: Aumentar penalización de quiebres
- **Mercados estables**: Reducir umbral CUSUM

---

## 10. Conclusiones

El sistema ABC implementa un pipeline estadístico robusto que:

1. **Detecta cambios estructurales** mediante CUSUM
2. **Incorpora información previa** mediante inferencia bayesiana
3. **Cuantifica incertidumbre** mediante simulación Monte Carlo
4. **Adapta automáticamente** a cambios de régimen
5. **Proporciona métricas de confianza** interpretables

La integración de tres metodologías complementarias proporciona un análisis más robusto que cualquier método individual, con mecanismos de retroalimentación que mejoran la calidad de las predicciones.

---

## Apéndice A: Glosario Estadístico

- **Drift (μ)**: Tendencia o momentum esperado de retornos
- **Volatilidad (σ)**: Desviación estándar de retornos (medida de riesgo)
- **Log-returns**: Retornos logarítmicos, r = ln(P_t/P_{t-1})
- **CUSUM**: Cumulative Sum, método de detección de cambios
- **Prior**: Distribución de probabilidad a priori (antes de observar datos)
- **Posterior**: Distribución de probabilidad a posteriori (después de observar datos)
- **Likelihood**: Verosimilitud, probabilidad de datos dado parámetros
- **GBM**: Geometric Brownian Motion, proceso estocástico para precios
- **VaR**: Value at Risk, pérdida máxima esperada a un nivel de confianza
- **CVaR**: Conditional VaR, pérdida esperada en el peor α% de casos
- **Conjugate Prior**: Prior que produce posterior de la misma familia
- **i.i.d.**: Independientes e idénticamente distribuidos

---

## Apéndice B: Fórmulas de Referencia Rápida

### Holt's Exponential Smoothing
```
L_t = α·P_t + (1-α)·(L_{t-1} + T_{t-1})
T_t = β·(L_t - L_{t-1}) + (1-β)·T_{t-1}
```

### Bayesian Update (Normal-Normal)
```
μ_post = (n_0·μ_0 + n·μ_sample) / (n_0 + n)
σ²_post = (n_0·σ²_0 + n·σ²_sample + n_0·n/(n_0+n)·(μ_sample-μ_0)²) / (n_0 + n)
```

### Geometric Brownian Motion
```
S_{t+Δt} = S_t · exp[(μ - 0.5·σ²)·Δt + σ·√Δt·Z],  Z ~ N(0,1)
```

### Value at Risk
```
VaR_α = S_0 - Percentil_α(S_final)
```

### Conditional VaR
```
CVaR_α = E[S_0 - S | S ≤ Percentil_α(S)]
```

---

**Versión**: 1.0  
**Fecha**: Abril 2026  
**Autor**: Sistema Analytics Service  
**Licencia**: Documentación Técnica Interna
