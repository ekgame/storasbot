package lt.ekgame.storasbot.plugins;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import lt.ekgame.beatmap_analyzer.Gamemode;
import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.beatmap.ctb.CatchBeatmap;
import lt.ekgame.beatmap_analyzer.beatmap.mania.ManiaBeatmap;
import lt.ekgame.beatmap_analyzer.beatmap.osu.OsuBeatmap;
import lt.ekgame.beatmap_analyzer.beatmap.taiko.TaikoBeatmap;
import lt.ekgame.beatmap_analyzer.difficulty.Difficulty;
import lt.ekgame.beatmap_analyzer.difficulty.DifficultyCalculator;
import lt.ekgame.beatmap_analyzer.difficulty.ManiaDifficulty;
import lt.ekgame.beatmap_analyzer.difficulty.ManiaDifficultyCalculator;
import lt.ekgame.beatmap_analyzer.difficulty.OsuDifficulty;
import lt.ekgame.beatmap_analyzer.difficulty.OsuDifficultyCalculator;
import lt.ekgame.beatmap_analyzer.difficulty.TaikoDifficulty;
import lt.ekgame.beatmap_analyzer.difficulty.TaikoDifficultyCalculator;
import lt.ekgame.beatmap_analyzer.performance.Performance;
import lt.ekgame.beatmap_analyzer.performance.scores.ManiaScore;
import lt.ekgame.beatmap_analyzer.performance.scores.OsuScore;
import lt.ekgame.beatmap_analyzer.performance.scores.TaikoScore;
import lt.ekgame.beatmap_analyzer.utils.Mod;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class BeatmapAnalyzer {
	
	private static final double ACC_BEGIN = 0.9;
	private static final double ACC_STEP = 0.005;
	
	private Beatmap beatmap;
	private DifficultyCalculator<?,?> calculator;
	
	public BeatmapAnalyzer(Beatmap beatmap) {
		this.beatmap = beatmap;
		switch (beatmap.getGamemode()) {
		case OSU:
			calculator = new OsuDifficultyCalculator();
			break;
		case TAIKO:
			calculator = new TaikoDifficultyCalculator();
			break;
		case CATCH:
			calculator = null;
			break;
		case MANIA:
			calculator = new ManiaDifficultyCalculator();
			break;
		case UNKNOWN:
			calculator = null;
			break;
		}
	}

	public BufferedImage getChartImage(int width, int height) {
		BufferedImage image = new BufferedImage(width, height*2, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		getStrainsChart().draw(graphics, new Rectangle2D.Double(0, 0, image.getWidth(), height));
		getPerformanceChart().draw(graphics, new Rectangle2D.Double(0, height, image.getWidth(), height));
		graphics.dispose();
		return image;
	}
	
	private JFreeChart getStrainsChart() {
		TimePeriodValuesCollection strainData = getStrainData(8);
		
		XYLineAndShapeRenderer renderer = new XYSplineRenderer();
		renderer.setSeriesShapesVisible(0, false);
		renderer.setSeriesShapesVisible(1, false);
		renderer.setSeriesShapesVisible(2, false);
		
        DateAxis domainAxis = new DateAxis(null);
        domainAxis.setDateFormatOverride(new SimpleDateFormat("mm:ss"));
        ValueAxis rangeAxis = new NumberAxis("Strain");
        
        XYPlot plot = new XYPlot(strainData, domainAxis, rangeAxis, renderer);
        JFreeChart chart = new JFreeChart("Beatmap strains", plot);
        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        return chart;
	}
	
	private JFreeChart getPerformanceChart() {
		XYLineAndShapeRenderer renderer = new XYSplineRenderer();
		for (int i = 0; i < 10; i++)
			renderer.setSeriesShapesVisible(i, false);
		
		NumberAxis rangeAxis = new NumberAxis("Accuracy");
		NumberAxis valueAxis = new NumberAxis("Performance");
		rangeAxis.setRange(ACC_BEGIN, 1);
		valueAxis.setAutoRange(true);
		valueAxis.setAutoRangeIncludesZero(false);
		DecimalFormat pctFormat = new DecimalFormat("#%");
		rangeAxis.setNumberFormatOverride(pctFormat);
		
        XYSeriesCollection performance = new XYSeriesCollection(getPerformanceData(Mods.NOMOD));
        performance.addSeries(getPerformanceData(new Mods(Mod.HIDDEN)));
        performance.addSeries(getPerformanceData(new Mods(Mod.HARDROCK)));
        performance.addSeries(getPerformanceData(new Mods(Mod.HIDDEN, Mod.HARDROCK)));
        
        XYPlot plotPerformance = new XYPlot(performance, rangeAxis, valueAxis, renderer);
        JFreeChart chart = new JFreeChart("Beatmap Performance", plotPerformance);
        chart.getLegend().setPosition(RectangleEdge.RIGHT);
        return chart;
	}
	
	private XYSeries getPerformanceData(Mods mods) {
		if (beatmap.getGamemode() == Gamemode.OSU)
			return calculateOsuPerformances((OsuBeatmap)beatmap, mods);
		else if (beatmap.getGamemode() == Gamemode.TAIKO)
			return calculateTaikoPerformances((TaikoBeatmap)beatmap, mods);
		else if (beatmap.getGamemode() == Gamemode.CATCH)
			return calculateCatchPerformances((CatchBeatmap)beatmap, mods);
		else if (beatmap.getGamemode() == Gamemode.MANIA)
			return calculateManiaPerformances((ManiaBeatmap)beatmap, mods);
		return null;
	}
	
	private XYSeries calculateOsuPerformances(OsuBeatmap beatmap, Mods mods) {
		XYSeries series = new XYSeries(getModsString(mods)); 
		OsuDifficulty difficulty = beatmap.getDifficulty(mods);
		double current = ACC_BEGIN;
		while (current <= 1) {
			if (current > 1) break;
			Performance performance = difficulty.getPerformance(OsuScore.of(beatmap).accuracy(current).build());
			series.add(current, performance.getPerformance());
			current += ACC_STEP;
		}
		return series;
	}
	
	private XYSeries calculateTaikoPerformances(TaikoBeatmap beatmap, Mods mods) {
		XYSeries series = new XYSeries(getModsString(mods));  
		TaikoDifficulty difficulty = beatmap.getDifficulty(mods);
		double current = ACC_BEGIN;
		while (current <= 1) {
			Performance performance = difficulty.getPerformance(TaikoScore.of(beatmap).accuracy(current).build());
			series.add(current, performance.getPerformance());
			current += ACC_STEP;
		}
		return series;
	}
	
	private XYSeries calculateCatchPerformances(CatchBeatmap beatmap, Mods mods) {
		XYSeries series = new XYSeries(getModsString(mods));  
		return series;
	}
	
	private XYSeries calculateManiaPerformances(ManiaBeatmap beatmap, Mods mods) {
		XYSeries series = new XYSeries(getModsString(mods));  
		ManiaDifficulty difficulty = beatmap.getDifficulty(mods);
		double current = ACC_BEGIN;
		while (current <= 1) {
			Performance performance = difficulty.getPerformance(ManiaScore.of(beatmap).accuracy(current).build());
			series.add(current, performance.getPerformance());
			current += ACC_STEP;
		}
		return series;
	}
	
	private String getModsString(Mods mods) {
		String str = mods.toString();
		if (str.isEmpty()) str = "No Mods";
		return str;
	}

	private TimePeriodValuesCollection getStrainData(int bucket) {
		TimePeriodValuesCollection collection = new TimePeriodValuesCollection();
		Difficulty<?, ?> difficulty = beatmap.getDifficulty();
		
		if (difficulty instanceof OsuDifficulty) {
			OsuDifficulty osuDiff = (OsuDifficulty) difficulty;
			
			List<Double> speedStrains = groupStrains(bucket, osuDiff.getSpeedStrains());
			List<Double> aimStrains = groupStrains(bucket, osuDiff.getAimStrains());
			
			TimePeriodValues overallSeries = getTimeSeries("Overall strain", 400*bucket, groupStrains(speedStrains, aimStrains));
			TimePeriodValues speedSeries = getTimeSeries("Speed strain", 400*bucket, speedStrains);
			TimePeriodValues aimSeries = getTimeSeries("Aim strain", 400*bucket, aimStrains);
			
			collection.addSeries(overallSeries);
			collection.addSeries(aimSeries);
			collection.addSeries(speedSeries);
		}
		else {
			TimePeriodValues starSeries = getTimeSeries("Overall strain", 400*bucket, groupStrains(bucket, difficulty.getStrains()));
			collection.addSeries(starSeries);
		}
		return collection;
	}

	private TimePeriodValues getTimeSeries(String name, int deltaTime, List<Double> values) {
		TimePeriodValues series = new TimePeriodValues(name);
		long time = 0;
		for (Double value : values) {
			series.add(new SimpleTimePeriod(time, time + 400), value);
			time += deltaTime;
		}
		return series;
	}
	
	private List<Double> groupStrains(int size, List<Double> strains) {
		List<Double> result = new ArrayList<>();
		Iterator<Double> iterator = strains.iterator();
		while (iterator.hasNext()) {
			List<Double> str = new ArrayList<>();
			for (int i = 0; i < size; i++)
				if (iterator.hasNext())
					str.add(iterator.next());
					
			result.add(calculator.calculateDifficulty(str));
		}
		return result;
	}
	
	private List<Double> groupStrains(List<Double> speedStrains, List<Double> aimStrains) {
		List<Double> result = new ArrayList<>();
		Iterator<Double> iteratorSpeed = speedStrains.iterator();
		Iterator<Double> iteratorAim = aimStrains.iterator();
		
		while (iteratorSpeed.hasNext() && iteratorAim.hasNext()) {
			Double speed = iteratorSpeed.next();
			Double aim = iteratorAim.next();
			result.add(aim + speed + Math.abs(speed - aim)*OsuDifficultyCalculator.EXTREME_SCALING_FACTOR);
		}
		
		return result;
	}
}
