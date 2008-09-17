package org.red5.io.mp4.impl;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.IKeyFrameMetaCache;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.impl.Tag;
import org.red5.io.mp4.MP4Atom;
import org.red5.io.mp4.MP4DataStream;
import org.red5.io.mp4.MP4Frame;
import org.red5.io.object.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This reader is used to read the contents of an MP4 file.
 * 
 * NOTE: This class is not implemented as thread-safe, the caller
 * should ensure the thread-safety.
 * <p>
 * New NetStream notifications
 * <br />
 * Two new notifications facilitate the implementation of the playback components:
 * <ul>
 * <li>NetStream.Play.FileStructureInvalid: This event is sent if the player detects 
 * an MP4 with an invalid file structure. Flash Player cannot play files that have 
 * invalid file structures.</li>
 * <li>NetStream.Play.NoSupportedTrackFound: This event is sent if the player does not 
 * detect any supported tracks. If there aren't any supported video, audio or data 
 * tracks found, Flash Player does not play the file.</li>
 * </ul>
 * </p>
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class MP4Reader implements IoConstants, ITagReader, IKeyFrameDataAnalyzer {

    /**
     * Logger
     */
    private static Logger log = LoggerFactory.getLogger(MP4Reader.class);

    /** Audio packet prefix */
	private final static byte[] PREFIX_AUDIO_FRAME = new byte[]{(byte) 0xaf, (byte) 0x01};

	/** Video packet prefix for key frames*/
	private final static byte[] PREFIX_VIDEO_KEYFRAME = new byte[]{(byte) 0x17, (byte) 0x01, (byte) 0, (byte) 0, (byte) 0};
    
	/** Video packet prefix for standard frames (interframe)*/
	private final static byte[] PREFIX_VIDEO_FRAME = new byte[]{(byte) 0x27, (byte) 0x01, (byte) 0, (byte) 0, (byte) 0};
	
	private final static byte[] CHUNK_MARKER = new byte[]{(byte) 0xc5};
    
    /**
     * File
     */
    private File file;
    
    /**
     * Input stream
     */
    private MP4DataStream fis;

    /**
     * File channel
     */
    private FileChannel channel;
    /**
     * Keyframe metadata
     */
	private KeyFrameMeta keyframeMeta;

	/** Mapping between file position and timestamp in ms. */
	private HashMap<Long, Long> posTimeMap;

	/** Mapping between file position and tag number. */
	private HashMap<Long, Integer> posTagMap;
	
	private HashMap<Integer, Long> samplePosMap;

	/** Cache for keyframe informations. */
	private static IKeyFrameMetaCache keyframeCache;
	
	/** Whether or not the clip contains a video track */
	private boolean hasVideo = false;	
	/** Whether or not the clip contains an audio track */
	private boolean hasAudio = false;
	
	//
	private String videoCodecId = "avc1";
	private String audioCodecId = "mp4a";
	
	/** Duration in milliseconds. */
	private long duration;	
	private int timeScale;
	private int width;
	private int height;
	private double audioSampleRate;
	private int audioChannels;
	private int videoSampleCount;
	private double fps;
	private double videoSampleRate = 2997.0; //not sure where to get this value from?
	private int avcLevel;
	private int avcProfile;
	private String formattedDuration;
	private long moovOffset;
	private long mdatOffset;
	
	//samples to chunk mappings
	private Vector videoSamplesToChunks;
	private Vector audioSamplesToChunks;
	//keyframe - sample numbers
	private Vector syncSamples;
	//samples 
	private Vector videoSamples;
	private Vector audioSamples;
	//chunk offsets
	private Vector videoChunkOffsets;
	private Vector audioChunkOffsets;
	//sample duration
	private int videoSampleDuration = 125;
	private int audioSampleDuration = 1024;
	
	//keep track of current sample
	private int currentSample = 1;
	
    private int prevFrameSize = 0;
	
    private List<MP4Frame> frames = new ArrayList<MP4Frame>();
    
	private long audioCount;
	private long videoCount;
	
	private double baseTs = 0f;
	
	/**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
	private LinkedList<ITag> firstTags = new LinkedList<ITag>();
	
	/** Constructs a new MP4Reader. */
	MP4Reader() {
	}

    /**
     * Creates MP4 reader from file input stream.
	 *
     * @param f         File
     */
    public MP4Reader(File f) throws IOException {
		this(f, false);
	}

    /**
     * Creates MP4 reader from file input stream, sets up metadata generation flag.
	 *
     * @param f                    File input stream
     * @param generateMetadata     <code>true</code> if metadata generation required, <code>false</code> otherwise
     */
    public MP4Reader(File f, boolean generateMetadata) throws IOException {
    	if (null == f) {
    		log.warn("Reader was passed a null file");
        	log.debug("{}", ToStringBuilder.reflectionToString(this));
    	}
    	this.file = f;
		this.fis = new MP4DataStream(new FileInputStream(f));
		channel = fis.getChannel();
		//decode all the info that we want from the atoms
		decodeHeader();
		//build the keyframe meta data
		analyzeKeyFrames();
		//add meta data
		//firstTags.add(createFileMeta());
		//create / add the pre-streaming tags
		createPreStreamingTags();
	}
    
	/**
	 * This handles the moov atom being at the beginning or end of the file, so the mdat may also
	 * be before or after the moov atom.
	 */
	public void decodeHeader() {
		try {
			// the first atom will/should be the type
			MP4Atom type = MP4Atom.createAtom(fis);
			// expect ftyp
			log.debug("Type {}", MP4Atom.intToType(type.getType()));
			//log.debug("Atom int types - free={} wide={}", MP4Atom.typeToInt("free"), MP4Atom.typeToInt("wide"));
			// keep a running count of the number of atoms found at the "top" levels
			int topAtoms = 0;
			// we want a moov and an mdat, anything else throw the invalid file type error
			while (topAtoms < 2) {
    			MP4Atom atom = MP4Atom.createAtom(fis);
    			switch (atom.getType()) {
    				case 1836019574: //moov
    					topAtoms++;
    					MP4Atom moov = atom;
    					// expect moov
    					log.debug("Type {}", MP4Atom.intToType(moov.getType()));
    					log.debug("moov children: {}", moov.getChildren());			
    					moovOffset = fis.getOffset() - moov.getSize();
    
    					MP4Atom mvhd = moov.lookup(MP4Atom.typeToInt("mvhd"), 0);
    					if (mvhd != null) {
    						log.debug("Movie header atom found");
    						log.debug("Time scale {} Duration {}", mvhd.getTimeScale(), mvhd.getDuration());
    						timeScale = mvhd.getTimeScale();
    						duration = mvhd.getDuration();
    					}
    
    					/* nothing needed here yet
    					MP4Atom meta = moov.lookup(MP4Atom.typeToInt("meta"), 0);
    					if (meta != null) {
    						log.debug("Meta atom found");
    						log.debug("{}", ToStringBuilder.reflectionToString(meta));
    					}
    					*/
    					
    					//two tracks or bust
    					int i = 0;
    					while (i < 2) {
    
    						MP4Atom trak = moov.lookup(MP4Atom.typeToInt("trak"), i);
    						if (trak != null) {
    							log.debug("Track atom found");
    							log.debug("trak children: {}", trak.getChildren());	
    							// trak: tkhd, edts, mdia
    							MP4Atom tkhd = trak.lookup(MP4Atom.typeToInt("tkhd"), 0);
    							if (tkhd != null) {
    								log.debug("Track header atom found");
    								log.debug("tkhd children: {}", tkhd.getChildren());	
    								if (tkhd.getWidth() > 0) {
    									width = tkhd.getWidth();
    									height = tkhd.getHeight();
    									log.debug("Width {} x Height {}", width, height);
    								}
    							}
    
    							MP4Atom edts = trak.lookup(MP4Atom.typeToInt("edts"), 0);
    							if (edts != null) {
    								log.debug("Edit atom found");
    								log.debug("edts children: {}", edts.getChildren());	
    								//log.debug("Width {} x Height {}", edts.getWidth(), edts.getHeight());
    							}					
    							
    							MP4Atom mdia = trak.lookup(MP4Atom.typeToInt("mdia"), 0);
    							if (mdia != null) {
    								log.debug("Media atom found");
    								// mdia: mdhd, hdlr, minf
    								MP4Atom hdlr = mdia
    										.lookup(MP4Atom.typeToInt("hdlr"), 0);
    								if (hdlr != null) {
    									log.debug("Handler ref atom found");
    									// soun or vide
    									log.debug("Handler type: {}", MP4Atom
    											.intToType(hdlr.getHandlerType()));
    									String hdlrType = MP4Atom.intToType(hdlr.getHandlerType());
    									if ("vide".equals(hdlrType)) {
    										hasVideo = true;
    									} else if ("soun".equals(hdlrType)) {
    										hasAudio = true;
    									}
    									i++;
    								}
    
    								MP4Atom minf = mdia
    										.lookup(MP4Atom.typeToInt("minf"), 0);
    								if (minf != null) {
    									log.debug("Media info atom found");
    									// minf: (audio) smhd, dinf, stbl / (video) vmhd,
    									// dinf, stbl
    
    									MP4Atom smhd = minf.lookup(MP4Atom
    											.typeToInt("smhd"), 0);
    									if (smhd != null) {
    										log.debug("Sound header atom found");
    										MP4Atom dinf = minf.lookup(MP4Atom
    												.typeToInt("dinf"), 0);
    										if (dinf != null) {
    											log.debug("Data info atom found");
    											// dinf: dref
    											log.debug("Sound dinf children: {}", dinf
    													.getChildren());
    											MP4Atom dref = dinf.lookup(MP4Atom
    													.typeToInt("dref"), 0);
    											if (dref != null) {
    												log.debug("Data reference atom found");
    											}
    
    										}
    										MP4Atom stbl = minf.lookup(MP4Atom
    												.typeToInt("stbl"), 0);
    										if (stbl != null) {
    											log.debug("Sample table atom found");
    											// stbl: stsd, stts, stss, stsc, stsz, stco,
    											// stsh
    											log.debug("Sound stbl children: {}", stbl
    													.getChildren());
    											// stsd - sample description
    											// stts - time to sample
    											// stsc - sample to chunk
    											// stsz - sample size
    											// stco - chunk offset
    
    											//stsd - has codec child
    											MP4Atom stsd = stbl.lookup(MP4Atom.typeToInt("stsd"), 0);
    											if (stsd != null) {
    												//stsd: mp4a
    												log.debug("Sample description atom found");
    												MP4Atom mp4a = stsd.getChildren().get(0);
    												//could set the audio codec here
    												setAudioCodecId(MP4Atom.intToType(mp4a.getType()));
    												//log.debug("{}", ToStringBuilder.reflectionToString(mp4a));
    												log.debug("Sample size: {}", mp4a.getSampleSize());										
    												audioSampleRate = mp4a.getTimeScale() * 1.0;
    												audioChannels = mp4a.getChannelCount();
    												log.debug("Sample rate (time scale): {}", audioSampleRate);			
    												log.debug("Channels: {}", audioChannels);										
    												/* no data we care about right now
    												//mp4a: esds
    												if (mp4a.getChildren().size() > 0) {
    													log.debug("Elementary stream descriptor atom found");
    													MP4Atom esds = mp4a.getChildren().get(0);
    													log.debug("{}", ToStringBuilder.reflectionToString(esds));
    													MP4Descriptor descriptor = esds.getEsd_descriptor();
    													//log.debug("{}", ToStringBuilder.reflectionToString(descriptor));
    													if (descriptor != null) {
    		    											Vector children = descriptor.getChildren();
    		    											for (int e = 0; e < children.size(); e++) { 
    		    												MP4Descriptor descr = (MP4Descriptor) children.get(e);
    		    												log.debug("{}", ToStringBuilder.reflectionToString(descr));
    		    												if (descr.getChildren().size() > 0) {
    		    													Vector children2 = descr.getChildren();
    		    													for (int e2 = 0; e2 < children2.size(); e2++) { 
    		    														MP4Descriptor descr2 = (MP4Descriptor) children2.get(e2);
    		    														log.debug("{}", ToStringBuilder.reflectionToString(descr2));														
    		    													}													
    		    												}
    		    											}
    													}
    												}
    												*/
    											}
    											//stsc - has Records
    											MP4Atom stsc = stbl.lookup(MP4Atom.typeToInt("stsc"), 0);
    											if (stsc != null) {
    												log.debug("Sample to chunk atom found");
    												audioSamplesToChunks = stsc.getRecords();
    												log.debug("Record count: {}", audioSamplesToChunks.size());
    												MP4Atom.Record rec = (MP4Atom.Record) audioSamplesToChunks.firstElement();
    												log.debug("Record data: Description index={} Samples per chunk={}", rec.getSampleDescriptionIndex(), rec.getSamplesPerChunk());
    											}									
    											//stsz - has Samples
    											MP4Atom stsz = stbl.lookup(MP4Atom.typeToInt("stsz"), 0);
    											if (stsz != null) {
    												log.debug("Sample size atom found");
    												audioSamples = stsz.getSamples();
    												//vector full of integers										
    												log.debug("Sample size: {}", stsz.getSampleSize());
    												log.debug("Sample count: {}", audioSamples.size());
    											}
    											//stco - has Chunks
    											MP4Atom stco = stbl.lookup(MP4Atom.typeToInt("stco"), 0);
    											if (stco != null) {
    												log.debug("Chunk offset atom found");
    												//vector full of integers
    												audioChunkOffsets = stco.getChunks();
    												log.debug("Chunk count: {}", audioChunkOffsets.size());
    												//set the first audio offset
    												//firstAudioChunkOffset = (Long) audioChunkOffsets.get(0);
    											}
    											//stts - has TimeSampleRecords
    											MP4Atom stts = stbl.lookup(MP4Atom.typeToInt("stts"), 0);
    											if (stts != null) {
    												log.debug("Time to sample atom found");
    												Vector records = stts.getTimeToSamplesRecords();
    												log.debug("Record count: {}", records.size());
    												MP4Atom.TimeSampleRecord rec = (MP4Atom.TimeSampleRecord) records.firstElement();
    												log.debug("Record data: Consecutive samples={} Duration={}", rec.getConsecutiveSamples(), rec.getSampleDuration());
    												//if we have 1 record then all samples have the same duration
    												if (records.size() > 1) {
    													//TODO: handle audio samples with varying durations
    													log.warn("Audio samples have differing durations, audio playback may fail");
    												}
    												audioSampleDuration = rec.getSampleDuration();
    											}		
    										}
    									}
    									MP4Atom vmhd = minf.lookup(MP4Atom
    											.typeToInt("vmhd"), 0);
    									if (vmhd != null) {
    										log.debug("Video header atom found");
    										MP4Atom dinf = minf.lookup(MP4Atom
    												.typeToInt("dinf"), 0);
    										if (dinf != null) {
    											log.debug("Data info atom found");
    											// dinf: dref
    											log.debug("Video dinf children: {}", dinf
    													.getChildren());
    											MP4Atom dref = dinf.lookup(MP4Atom
    													.typeToInt("dref"), 0);
    											if (dref != null) {
    												log.debug("Data reference atom found");
    											}
    										}
    										MP4Atom stbl = minf.lookup(MP4Atom
    												.typeToInt("stbl"), 0);
    										if (stbl != null) {
    											log.debug("Sample table atom found");
    											// stbl: stsd, stts, stss, stsc, stsz, stco,
    											// stsh
    											log.debug("Video stbl children: {}", stbl
    													.getChildren());
    											// stsd - sample description
    											// stts - (decoding) time to sample
    											// stsc - sample to chunk
    											// stsz - sample size
    											// stco - chunk offset
    											// ctts - (composition) time to sample
    											// stss - sync sample
    											// sdtp - independent and disposable samples
    
    											//stsd - has codec child
    											MP4Atom stsd = stbl.lookup(MP4Atom.typeToInt("stsd"), 0);
    											if (stsd != null) {
    												log.debug("Sample description atom found");
    												MP4Atom avc1 = stsd.getChildren().get(0);
    												//could set the video codec here
    												setVideoCodecId(MP4Atom.intToType(avc1.getType()));
    												log.debug("Sample rate (time scale): {}", videoSampleRate);
    												//
    												MP4Atom avcC = avc1.lookup(MP4Atom.typeToInt("avcC"), 0);
    												if (avcC != null) {
    													avcLevel = avcC.getAvcLevel();
    													log.debug("AVC level: {}", avcLevel);
    													avcProfile = avcC.getAvcProfile();
    													log.debug("AVC Profile: {}", avcProfile);
    												}
    												log.debug("{}", ToStringBuilder.reflectionToString(avc1));
    											}
    											//stsc - has Records
    											MP4Atom stsc = stbl.lookup(MP4Atom.typeToInt("stsc"), 0);
    											if (stsc != null) {
    												log.debug("Sample to chunk atom found");
    												videoSamplesToChunks = stsc.getRecords();
    												log.debug("Record count: {}", videoSamplesToChunks.size());
    												MP4Atom.Record rec = (MP4Atom.Record) videoSamplesToChunks.firstElement();
    												log.debug("Record data: Description index={} Samples per chunk={}", rec.getSampleDescriptionIndex(), rec.getSamplesPerChunk());
    											}									
    											//stsz - has Samples
    											MP4Atom stsz = stbl.lookup(MP4Atom.typeToInt("stsz"), 0);
    											if (stsz != null) {
    												log.debug("Sample size atom found");
    												//vector full of integers							
    												videoSamples = stsz.getSamples();
    												//if sample size is 0 then the table must be checked due
    												//to variable sample sizes
    												log.debug("Sample size: {}", stsz.getSampleSize());
    												videoSampleCount = videoSamples.size();
    												log.debug("Sample count: {}", videoSampleCount);
    											}
    											//stco - has Chunks
    											MP4Atom stco = stbl.lookup(MP4Atom.typeToInt("stco"), 0);
    											if (stco != null) {
    												log.debug("Chunk offset atom found");
    												//vector full of integers
    												videoChunkOffsets = stco.getChunks();
    												log.debug("Chunk count: {}", videoChunkOffsets.size());
    												//set the first video offset
    												//firstVideoChunkOffset = (Long) videoChunkOffsets.get(0);
    											}									
    											//stss - has Sync - no sync means all samples are keyframes
    											MP4Atom stss = stbl.lookup(MP4Atom.typeToInt("stss"), 0);
    											if (stss != null) {
    												log.debug("Sync sample atom found");
    												//vector full of integers
    												syncSamples = stss.getSyncSamples();
    												log.debug("Keyframes: {}", syncSamples.size());
    											}		
    											//stts - has TimeSampleRecords
    											MP4Atom stts = stbl.lookup(MP4Atom.typeToInt("stts"), 0);
    											if (stts != null) {
    												log.debug("Time to sample atom found");
    												Vector records = stts.getTimeToSamplesRecords();
    												log.debug("Record count: {}", records.size());
    												MP4Atom.TimeSampleRecord rec = (MP4Atom.TimeSampleRecord) records.firstElement();
    												log.debug("Record data: Consecutive samples={} Duration={}", rec.getConsecutiveSamples(), rec.getSampleDuration());
    												//if we have 1 record then all samples have the same duration
    												if (records.size() > 1) {
    													//TODO: handle video samples with varying durations
    													log.warn("Video samples have differing durations, video playback may fail");
    												}
    												videoSampleDuration = rec.getSampleDuration();
    											}										
    										}
    									}
    
    								}
    
    							}
    						}
    					}
    					//calculate FPS
    					fps = (videoSampleCount * timeScale) / (double) duration;
    					log.debug("FPS calc: ({} * {}) / {}", new Object[]{videoSampleCount, timeScale, duration});
    					log.debug("FPS: {}", fps);
    						
    					//real duration
    					StringBuilder sb = new StringBuilder();
    					double videoTime = ((double) duration / (double) timeScale);
    					log.debug("Video time: {}", videoTime);
    					int minutes = (int) (videoTime / 60);
    					if (minutes > 0) {
    		    			sb.append(minutes);
    		    			sb.append('.');
    					}
    					//formatter for seconds / millis
    					NumberFormat df = DecimalFormat.getInstance();
    					df.setMaximumFractionDigits(2);
    					sb.append(df.format((videoTime % 60)));
    					formattedDuration = sb.toString();
    					log.debug("Time: {}", formattedDuration);				
    
    					break;
    				case 1835295092: //mdat
    					topAtoms++;
    					long dataSize = 0L;
    					MP4Atom mdat = atom;	    				
	    				dataSize = mdat.getSize();
	    				log.debug("{}", ToStringBuilder.reflectionToString(mdat));    
	    				mdatOffset = fis.getOffset() - dataSize;
    					log.debug("File size: {} mdat size: {}", file.length(), dataSize);
    					
    					break;
    				case 1718773093: //free
    				case 2003395685: //wide
    					break;
    				default:
    					log.warn("Unexpected atom: {}", MP4Atom.intToType(atom.getType()));
    			}
			}

			//add the tag name (size) to the offsets
			moovOffset += 8;
			mdatOffset += 8;
			log.debug("Offsets moov: {} mdat: {}", moovOffset, mdatOffset);
						
		} catch (IOException e) {
			log.error("{}", e);
		}		
	}
	
    public void setKeyFrameCache(IKeyFrameMetaCache keyframeCache) {
    	MP4Reader.keyframeCache = keyframeCache;
    }

    /**
	 * Get the remaining bytes that could be read from a file or ByteBuffer.
	 *
	 * @return          Number of remaining bytes
	 */
	private long getRemainingBytes() {
		try {
			return channel.size() - channel.position();
		} catch (Exception e) {
			log.error("Error getRemainingBytes", e);
			return 0;
		}
	}

	/**
	 * Get the total readable bytes in a file or ByteBuffer.
	 *
	 * @return          Total readable bytes
	 */
	public long getTotalBytes() {
		try {
			return channel.size();
		} catch (Exception e) {
			log.error("Error getTotalBytes", e);
		} 
		if (file != null) {
			//just return the file size
			return file.length();
		} else {
			return 0;
		}
	}

	/**
	 * Get the current position in a file or ByteBuffer.
	 *
	 * @return           Current position in a file
	 */
	private long getCurrentPosition() {
		try {
			//if we are at the end of the file drop back to mdat offset
			if (channel.position() == channel.size()) {
				log.debug("Reached end of file, going back to data offset");
				channel.position(mdatOffset);
			}		
			return channel.position();
		} catch (Exception e) {
			log.error("Error getCurrentPosition", e);
			return 0;
		}
	}

    /** {@inheritDoc} */
    public boolean hasVideo() {
    	return hasVideo;
    }

	/**
	 * Returns the file buffer.
	 * 
	 * @return  File contents as byte buffer
	 */
	public ByteBuffer getFileData() {
		// TODO as of now, return null will disable cache
		// we need to redesign the cache architecture so that
		// the cache is layered underneath FLVReader not above it,
		// thus both tag cache and file cache are feasible.
		return null;
	}

	/** {@inheritDoc}
	 */
	public IStreamableFile getFile() {
		// TODO wondering if we need to have a reference
		return null;
	}

	/** {@inheritDoc}
	 */
	public int getOffset() {
		// XXX what's the difference from getBytesRead
		return 0;
	}

	/** {@inheritDoc}
	 */
	public long getBytesRead() {
		// XXX should summarize the total bytes read or
		// just the current position?
		return getCurrentPosition();
	}

	/** {@inheritDoc} */
    public long getDuration() {
		return duration;
	}

	public String getVideoCodecId() {
		return videoCodecId;
	}

	public String getAudioCodecId() {
		return audioCodecId;
	}

	/** {@inheritDoc}
	 */
	public boolean hasMoreTags() {
		return currentSample < frames.size();
	}

    /**
     * Create tag for metadata event.
	 *
	 * Info from http://www.kaourantin.net/2007/08/what-just-happened-to-video-on-web_20.html
	 * <pre>
		duration - Obvious. But unlike for FLV files this field will always be present.
		videocodecid - For H.264 we report 'avc1'.
        audiocodecid - For AAC we report 'mp4a', for MP3 we report '.mp3'.
        avcprofile - 66, 77, 88, 100, 110, 122 or 144 which corresponds to the H.264 profiles.
        avclevel - A number between 10 and 51. Consult this list to find out more.
        aottype - Either 0, 1 or 2. This corresponds to AAC Main, AAC LC and SBR audio types.
        moovposition - The offset in bytes of the moov atom in a file.
        trackinfo - An array of objects containing various infomation about all the tracks in a file
          ex.
        	trackinfo[0].length: 7081
        	trackinfo[0].timescale: 600
        	trackinfo[0].sampledescription.sampletype: avc1
        	trackinfo[0].language: und
        	trackinfo[1].length: 525312
        	trackinfo[1].timescale: 44100
        	trackinfo[1].sampledescription.sampletype: mp4a
        	trackinfo[1].language: und
        
        chapters - As mentioned above information about chapters in audiobooks.
        seekpoints - As mentioned above times you can directly feed into NetStream.seek();
        videoframerate - The frame rate of the video if a monotone frame rate is used. 
        		Most videos will have a monotone frame rate.
        audiosamplerate - The original sampling rate of the audio track.
        audiochannels - The original number of channels of the audio track.
        tags - As mentioned above ID3 like tag information.
	 * </pre>
	 * Info from 
	 * <pre>
		width: Display width in pixels.
		height: Display height in pixels.
		duration: Duration in seconds.
		avcprofile: AVC profile number such as 55, 77, 100 etc.
		avclevel: AVC IDC level number such as 10, 11, 20, 21 etc.
		aacaot: AAC audio object type; 0, 1 or 2 are supported.
		videoframerate: Frame rate of the video in this MP4.
		seekpoints: Array that lists the available keyframes in a file as time stamps in milliseconds. 
				This is optional as the MP4 file might not contain this information. Generally speaking, 
				most MP4 files will include this by default.
		videocodecid: Usually a string such as "avc1" or "VP6F."
		audiocodecid: Usually a string such as ".mp3" or "mp4a."
		progressivedownloadinfo: Object that provides information from the "pdin" atom. This is optional 
				and many files will not have this field.
 		trackinfo: Object that provides information on all the tracks in the MP4 file, including their 
 				sample description ID.
		tags: Array of key value pairs representing the information present in the "ilst" atom, which is 
				the equivalent of ID3 tags for MP4 files. These tags are mostly used by iTunes. 
	 * </pre>
	 *
     * @return         Metadata event tag
     */
    ITag createFileMeta() {
    	log.debug("Creating onMetaData");
		// Create tag for onMetaData event
		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		Map<Object, Object> props = new HashMap<Object, Object>();
        // Duration property
		props.put("duration", ((double) duration / (double) timeScale));
		props.put("width", width);
		props.put("height", height);

		// Video codec id
		props.put("videocodecid", videoCodecId);
		props.put("avcprofile", avcProfile);
        props.put("avclevel", avcLevel);
        props.put("videoframerate", fps);
		// Audio codec id - watch for mp3 instead of aac
        props.put("audiocodecid", audioCodecId);
        props.put("aacaot", 2);
        props.put("audiosamplerate", audioSampleRate);
        props.put("audiochannels", audioChannels);
        
        props.put("moovposition", moovOffset);
        //props.put("chapters", "");
        props.put("seekpoints", syncSamples);
        //tags will only appear if there is an "ilst" atom in the file
        //props.put("tags", "");
   
//        Object[] arr = new Object[2];
//        Map<String, Object> audioMap = new HashMap<String, Object>(4);
//        audioMap.put("timescale", audioSampleRate);
//        audioMap.put("language", "eng");
//        audioMap.put("length", Integer.valueOf(10552320));
//        Map<String, String> sampleMap = new HashMap<String, String>(1);
//        sampleMap.put("sampletype", audioCodecId);
//        audioMap.put("sampledescription", sampleMap);
//        arr[0] = audioMap;
//        Map<String, Object> videoMap = new HashMap<String, Object>(3);
//        videoMap.put("timescale", Integer.valueOf(2997));
//        videoMap.put("language", "eng");
//        videoMap.put("length", Integer.valueOf(717125));
//        arr[1] = videoMap;
//        props.put("trackinfo", arr);
        
		//props.put("canSeekToEnd", false);
		out.writeMap(props, new Serializer());
		buf.flip();

		//now that all the meta properties are done, update the duration
		duration = Math.round(duration * 1000d);
		
		ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
		result.setBody(buf);
		return result;
	}

    /**
	 * Tag sequence
	 * Notify - onMetaData
	 * Video - ts=0 size=2 bytes {52 00}
	 * Video - ts=0 size=5 bytes {17 02 00 00 00}
	 * Video - ts=0 size=2 bytes {52 01}
	 * Audio - ts=0 size=4 bytes {af 00 12 10}
	 * Audio - ts=0 size=9 bytes {af 01 20 00 00 00 00 00 0e} 
	 * 
	 * Packet prefixes:
	 * 17 00 00 00 00 = Video extra data (first video packet)
	 * 17 01 00 00 00 = Keyframe
	 * 27 01 00 00 00 = Interframe
	 * af 00 = Audio extra data (first audio packet)
	 * af 01 = Audio
	 * 
	 * Audio extra data(s):
	 * af 00 12 10 06 = AAC LC
	 * af 00 13 90 56 e5 a5 48 00 = HE-AAC
	 */
    private void createPreStreamingTags() {
    	log.debug("Creating pre-streaming tags");
    	ITag tag = null;
    	ByteBuffer body = null;
    	
    	if (hasVideo) {
        	//video tag #1
        	tag = new Tag(IoConstants.TYPE_VIDEO, 0, 43, null, 0);
    		body = ByteBuffer.allocate(tag.getBodySize());
    		body.put(new byte[]{(byte) 0x17, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
    		(byte) 0x01, (byte) 0x4d, (byte) 0x40, (byte) 0x33, (byte) 0xff, (byte) 0xff, (byte) 0,
    		(byte) 0x17, (byte) 0x67, (byte) 0x4d, (byte) 0x40, (byte) 0x33, (byte) 0x9a, (byte) 0x76,
    		(byte) 0x02, (byte) 0x80, (byte) 0x2d, (byte) 0xd0, (byte) 0x80, (byte) 0,    (byte) 0,
    		(byte) 0x03, (byte) 0,    (byte) 0x80, (byte) 0,    (byte) 0,    (byte) 0x19, (byte) 0x47,
    		(byte) 0x8c, (byte) 0x18, (byte) 0x9c, (byte) 0x01, (byte) 0,    (byte) 0x04, (byte) 0x68,
    		(byte) 0xce, (byte) 0x3c, (byte) 0x80});
    		
    		//fake avcc
    		//(byte) 0x01, (byte) 0x4D, (byte) 0x40, (byte) 0x1F, (byte) 0xFF, (byte) 0xE1, (byte) 0x00,
    		//(byte) 0x14, (byte) 0x27, (byte) 0x4D, (byte) 0x40, (byte) 0x1F, (byte) 0xA9, (byte) 0x18,
    		//(byte) 0x0A, (byte) 0x00, (byte) 0x8B, (byte) 0x60, (byte) 0x0D, (byte) 0x41, (byte) 0x80,
    		//(byte) 0x41, (byte) 0x8C, (byte) 0x2B, (byte) 0x5E, (byte) 0xF7, (byte) 0xC0, (byte) 0x40,
    		//(byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x28, (byte) 0xCE, (byte) 0x09, (byte) 0xC8

    		body.flip();
    		tag.setBody(body);
    
    		//add tag
    		firstTags.add(tag);
    	}
    	
    	if (hasAudio) {
    		//audio tag #1
    		tag = new Tag(IoConstants.TYPE_AUDIO, 0, 5, null, tag.getBodySize());
    		body = ByteBuffer.allocate(tag.getBodySize());
    		body.put(new byte[]{(byte) 0xaf, (byte) 0, (byte) 0x12, (byte) 0x10, (byte) 0x06});
    		body.flip();
    		tag.setBody(body);
    
    		//add tag
    		firstTags.add(tag);
    	}
    }
    
	/**
	 * 
	 */
    public synchronized ITag readTag() {
		log.debug("Read tag");
		//empty-out the pre-streaming tags first
		if (!firstTags.isEmpty()) {
			log.debug("Returning pre-tag");
			// Return first tags before media data
			return firstTags.removeFirst();
		}		
		log.debug("Read tag - sample {} prevFrameSize {} audio: {} video: {}", new Object[]{currentSample, prevFrameSize, audioCount, videoCount});
		
		//get the current frame
		MP4Frame frame = frames.get(currentSample - 1);
		log.debug("Playback {}", frame);
		
		int sampleSize = frame.getSize();
		
		double frameTs = (frame.getTime() - baseTs) * 1000.0;
		int time = (int) Math.round(frameTs);
		log.debug("Read tag - dst: {} base: {} time: {}", new Object[]{frameTs, baseTs, time});
		//log.debug("Read tag - sampleSize {} ts {}", new Object[]{sampleSize, ts});
		//log.debug("Read tag - sample dur / scale {}", new Object[]{((currentSample * timeScale) / videoSampleDuration)});		
		
		long samplePos = frame.getOffset();
		//log.debug("Read tag - samplePos {}", samplePos);

		//determine frame type and packet body padding
		byte type = frame.getType();
		//assume video type
		int pad = 5;
		if (type == TYPE_AUDIO) {
			pad = 2;
		}

		//create a byte buffer of the size of the sample
		java.nio.ByteBuffer data = java.nio.ByteBuffer.allocate(sampleSize + pad);
		try {
			//prefix is different for keyframes
			if (type == TYPE_VIDEO) {
	    		if (frame.isKeyFrame()) {
	    			log.debug("Writing keyframe prefix");
	    			data.put(PREFIX_VIDEO_KEYFRAME);
	    		} else {  			
	    			log.debug("Writing interframe prefix");
	    			data.put(PREFIX_VIDEO_FRAME);
	    		}
	    		
	    		videoCount++;
			} else {
				log.debug("Writing audio prefix");
				data.put(PREFIX_AUDIO_FRAME);
				
				audioCount++;
			}
			//do we need to add the mdat offset to the sample position?
			channel.position(samplePos);
			channel.read(data);
		} catch (IOException e) {
			log.error("Error on channel position / read", e);
		}
		
		//chunk the data
		ByteBuffer payload = getChunkedPayload(data.array());		
		
		//create the tag
		ITag tag = new Tag(type, time, payload.limit(), payload, prevFrameSize);
		log.debug("Read tag - type: {} body size: {}", (type == TYPE_AUDIO ? "Audio" : "Video"), tag.getBodySize());
		
		//increment the sample number
		currentSample++;			
		//set the frame / tag size
		prevFrameSize = tag.getBodySize();
	
		baseTs += frameTs / 1000.0;
		//log.debug("Tag: {}", tag);
		return tag;
	}

    /**
     * Performs frame analysis and generates metadata for use in seeking. The
     * method name is a little misleading since it analyzes all the frames.
	 *
     * @return             Keyframe metadata
     */
    public KeyFrameMeta analyzeKeyFrames() {
		if (keyframeMeta != null) {
			log.debug("Key frame meta already generated");
			return keyframeMeta;
		}
		log.debug("Analyzing key frames");
					
		//key frame sample numbers are stored in the syncSamples collection
		int keyframeCount = syncSamples.size();
		
        // Lists of video positions and timestamps
        List<Long> positionList = new ArrayList<Long>(keyframeCount);
        List<Float> timestampList = new ArrayList<Float>(keyframeCount);     
        // Maps positions to tags
        posTagMap = new HashMap<Long, Integer>();
        samplePosMap = new HashMap<Integer, Long>();
        // tag == sample
		int sample = 1;
		Long pos = null;
		Enumeration records = videoSamplesToChunks.elements();
		while (records.hasMoreElements()) {
			MP4Atom.Record record = (MP4Atom.Record) records.nextElement();
			int firstChunk = record.getFirstChunk();
			int sampleCount = record.getSamplesPerChunk();
			log.debug("Video first chunk: {} count:{}", firstChunk, sampleCount);
			pos = (Long) videoChunkOffsets.elementAt(firstChunk - 1);
			while (sampleCount > 0) {
				//log.debug("Position: {}", pos);
    			posTagMap.put(pos, sample);
    			samplePosMap.put(sample, pos);
				//calculate ts
    			double ts = (videoSampleDuration * (sample - 1)) / videoSampleRate;
    			//check to see if the sample is a keyframe
    			boolean keyframe = syncSamples.contains(sample);
    			if (keyframe) {
    				log.debug("Keyframe - sample: {}", sample);
    				positionList.add(pos);
    				//log.debug("Keyframe - timestamp: {}", ts);
    				//timestampList.add(ts);
    			}
    			int size = ((Integer) videoSamples.get(sample - 1)).intValue();

    			//create a frame
    			MP4Frame frame = new MP4Frame();
    			frame.setKeyFrame(keyframe);
    			frame.setOffset(pos);
    			frame.setSize(size);
    			frame.setTime(ts);
    			frame.setType(TYPE_VIDEO);
    			frames.add(frame);
    			
    			log.debug("Sample #{} {}", sample, frame);
    			
    			//inc and dec stuff
    			pos += size;
    			sampleCount--;
    			sample++;    			
			}
		}

		log.debug("Position map size: {} keyframe list size: {}", posTagMap.size(), positionList.size());
		log.debug("Sample position map (video): {}", samplePosMap);
			
		//add the audio frames / samples / chunks		
		sample = 1;
		records = audioSamplesToChunks.elements();
		while (records.hasMoreElements()) {
			MP4Atom.Record record = (MP4Atom.Record) records.nextElement();
			int firstChunk = record.getFirstChunk();
			int sampleCount = record.getSamplesPerChunk();
			log.debug("Audio first chunk: {} count:{}", firstChunk, sampleCount);
			pos = (Long) audioChunkOffsets.elementAt(firstChunk - 1);
			while (sampleCount > 0) {
    			//calculate ts
				double ts = (audioSampleDuration * (sample - 1)) / audioSampleRate;
    			//sample size
    			int size = ((Integer) audioSamples.get(sample - 1)).intValue();
    			//create a frame
        		MP4Frame frame = new MP4Frame();
        		frame.setOffset(pos);
        		frame.setSize(size);
        		frame.setTime(ts);
        		frame.setType(TYPE_AUDIO);
        		frames.add(frame);
        		
    			log.debug("Sample #{} {}", sample, frame);
    			
    			//inc and dec stuff
    			pos += size;
    			sampleCount--;
    			sample++;    
            }		
		}

		records = null;
		
		//sort the frames
		Collections.sort(frames);
		
		log.debug("Frames count (expect 16042 for backcountry): {}", frames.size());
		log.debug("Frames: {}", frames);
		
		keyframeMeta = new KeyFrameMeta();
		keyframeMeta.duration = duration;
		/*
		posTimeMap = new HashMap<Long, Long>();

		keyframeMeta.positions = new long[positionList.size()];
		keyframeMeta.timestamps = new float[timestampList.size()];
		for (int i = 0; i < keyframeMeta.positions.length; i++) {
			keyframeMeta.positions[i] = positionList.get(i);
			keyframeMeta.timestamps[i] = timestampList.get(i);
			posTimeMap.put((long) positionList.get(i), (float) timestampList
					.get(i));
		}
		if (keyframeCache != null) {
			keyframeCache.saveKeyFrameMeta(file, keyframeMeta);
		}
		*/
		return keyframeMeta;
	}

    /**
     * Handler for data chunks.
     * 
     * @param payload
     * @return
     */
    private ByteBuffer getChunkedPayload(byte[] payload) {
    	//log.debug("Get chunked payload");
    	int len = payload.length;
    	//log.debug("Payload length: {}", len);
    	int chunkLen = 0;
    	int offset = 0;
    	//extra bytes needed for chunk markers and such
    	int extra = Math.max(0, Math.round(len / 4096));
    	//size the return array as good as possible to prevent resize
    	ByteBuffer ret = ByteBuffer.allocate(len + extra);
    	//allow resize
    	ret.setAutoExpand(true);
    	while (len > 0) {
        	chunkLen = Math.min(len, 4096);
        	//log.debug("Chunk len: {}", chunkLen);
        	ret.put(payload, offset, chunkLen);
        	//log.debug("read: {}", ret.position());
        	offset += chunkLen;
        	len -= chunkLen;
        	//log.debug("len: {}", len);
        	if (len > 0) {
        		ret.put(CHUNK_MARKER);
        	}
    	}
    	ret.flip();
    	return ret;
    }
    
	/**
	 * Put the current position to pos.
	 * The caller must ensure the pos is a valid one
	 * (eg. not sit in the middle of a frame).
	 *
	 * @param pos         New position in file. Pass <code>Long.MAX_VALUE</code> to seek to end of file.
	 */
	public void position(long pos) {
		log.debug("position: {}", pos);
	}

	/** {@inheritDoc}
	 */
	public void close() {
		log.debug("Close");
		if (channel != null) {
			try {
				channel.close();
				fis.close();
				fis = null;
			} catch (IOException e) {
				log.error("Channel close {}", e);
			}
		}
	}

	public void setVideoCodecId(String videoCodecId) {
		this.videoCodecId = videoCodecId;
	}

	public void setAudioCodecId(String audioCodecId) {
		this.audioCodecId = audioCodecId;
	}

	public ITag readTagHeader() {
		return null;
	}
	
}
