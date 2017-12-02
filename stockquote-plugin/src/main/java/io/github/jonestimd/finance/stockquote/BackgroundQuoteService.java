// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import com.typesafe.config.Config;
import org.apache.log4j.Logger;

public class BackgroundQuoteService {
    private final Logger logger = Logger.getLogger(getClass());
    private final ExecutorService executorService;
    private final String threadNamePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final List<StockQuoteService> quoteServices;

    public BackgroundQuoteService(Config config, List<StockQuoteService> quoteServices) {
        this.quoteServices = quoteServices;
        int coreSize = config.getInt("threadPool.coreSize");
        int maxSize = config.getInt("threadPool.maxSize");
        int keepAlive = config.getInt("threadPool.keepAliveSeconds");
        this.threadNamePrefix = config.getString("threadPool.namePrefix");
        this.executorService = new ThreadPoolExecutor(coreSize, maxSize, keepAlive, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), this::newThread);
    }

    public void getPrices(Collection<String> symbols, Consumer<Stream<StockQuote>> callback) {
        executorService.submit(new Worker(symbols, callback));
    }

    private Thread newThread(Runnable runnable) {
        Thread thread = new Thread(null, runnable, threadNamePrefix + threadNumber.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }

    private class Worker implements Runnable {
        private final Collection<String> symbols;
        private final Consumer<Stream<StockQuote>> callback;

        public Worker(Collection<String> symbols, Consumer<Stream<StockQuote>> callback) {
            this.symbols = symbols;
            this.callback = callback;
        }

        @Override
        public void run() {
            for (StockQuoteService quoteService : quoteServices) {
                try {
                    quoteService.getPrices(new HashSet<>(symbols), this::updateUI);
                    if (symbols.isEmpty()) break;
                } catch (Exception ex) {
                    logger.error("Error getting prices", ex);
                }
            }
            if (! symbols.isEmpty()) {
                SwingUtilities.invokeLater(() -> callback.accept(symbols.stream().map(StockQuote::notAvailable)));
            }
        }

        private void updateUI(Map<String, BigDecimal> prices) {
            symbols.removeAll(prices.keySet());
            SwingUtilities.invokeLater(() -> callback.accept(StockQuote.fromMap(prices)));
        }
    }
}