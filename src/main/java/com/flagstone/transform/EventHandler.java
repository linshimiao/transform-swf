/*
 * Event.java
 * Transform
 *
 * Copyright (c) 2001-2010 Flagstone Software Ltd. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Flagstone Software Ltd. nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.flagstone.transform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.flagstone.transform.action.Action;
import com.flagstone.transform.action.ActionData;
import com.flagstone.transform.coder.Context;
import com.flagstone.transform.coder.MovieTypes;
import com.flagstone.transform.coder.SWFDecoder;
import com.flagstone.transform.coder.SWFEncodeable;
import com.flagstone.transform.coder.SWFEncoder;
import com.flagstone.transform.coder.SWFFactory;

/**
 * <p>
 * ClipEvent is used to define the actions that a movie clip will execute in
 * response to a particular event. ClipEvent objects are added to an
 * Place2 object and the actions are registered with the Flash Player
 * when the movie clip is added to the display list.
 * </p>
 *
 * <p>
 * The events that a movie clip responds to are:
 * </p>
 *
 * <table class="datasheet">
 * <tr>
 * <td valign="top">Load</td>
 * <td>the movie clip is finished loading.</td>
 * </tr>
 * <tr>
 * <td valign="top">Unload</td>
 * <td>the movie clip is unloaded from the parent movie.</td>
 * </tr>
 * <tr>
 * <td valign="top">EnterFrame</td>
 * <td>when the mouse enters the Flash Player window.</td>
 * </tr>
 * <tr>
 * <td valign="top">MouseMove</td>
 * <td>the mouse pointer is moved.</td>
 * </tr>
 * <tr>
 * <td valign="top">MouseDown</td>
 * <td>the left mouse button is pressed while the cursor is outside of the
 * bounding rectangle of the movie clip.</td>
 * </tr>
 * <tr>
 * <td valign="top">MouseUp</td>
 * <td>the left mouse button is pressed and released while the cursor is outside
 * of the bounding rectangle of the movie clip.</td>
 * </tr>
 * <tr>
 * <td valign="top">KeyDown</td>
 * <td>a key is pressed on the keyboard. From Flash 6 a key code can be
 * specified to identify a specific key rather than testing for the value inside
 * the actions that are executed in response to the event.</td>
 * </tr>
 * <tr>
 * <td valign="top">KeyUp</td>
 * <td>akey being pressed on the keyboard is released.</td>
 * </tr>
 * <tr>
 * <td valign="top">Data</td>
 * <td>
 * a GetUrl2 action is executed with the movie clip specified as a target.
 * </td>
 * </tr>
 * <tr>
 * <td valign="top">Construct</td>
 * <td>This event is not documented by Adobe.</td>
 * </tr>
 * </table>
 *
 * <p>
 * Starting with Flash 6 movie clips also respond to the same set of events as
 * buttons, see ButtonEventHandler
 * </p>
 *
 * <p>
 * A ClipEvent object can define the actions that will be executed in response
 * to more than one event, simply bitwise OR together the individual event
 * codes:
 * </p>
 *
 * <pre>
 * int loadAndMouseMove = ClipEvent.Load | ClipEvent.MouseMove;
 * </pre>
 */
//TODO(class)
public final class EventHandler implements SWFEncodeable {

    /** Format string used in toString() method. */
    private static final String FORMAT = "EventHandler: { events=%s;"
            + " key=%s; actions=%s }";

    /** Version og Flash that supports the extended event model. */
    private static final int VERSION_WITH_EXT_EVENTS = 6;

    /** Number of bits to shift key code for encoding with event flags. */
    private static final int KEY_OFFSET = 9;
    /** Bit mask for key field. */
    private static final int KEY_MASK = 0xFE00;
    /** Bit mask for key field. */
    private static final int EVENT_MASK = 0x01FF;

    private static final int NUM_BUTTON_EVENTS = 9;
    private static final int NUM_CLIP_EVENTS = 19;

    private static final int BIT0 = 1;
    private static final int BIT1 = 2;
    private static final int BIT2 = 4;
    private static final int BIT3 = 8;
    private static final int BIT4 = 16;
    private static final int BIT5 = 32;
    private static final int BIT6 = 64;
    private static final int BIT7 = 128;
    private static final int BIT8 = 256;
    private static final int BIT9 = 512;
    private static final int BIT10 = 1024;
    private static final int BIT11 = 2048;
    private static final int BIT12 = 4096;
    private static final int BIT13 = 8192;
    private static final int BIT14 = 16384;
    private static final int BIT15 = 32768;
    private static final int BIT16 = 65536;
    private static final int BIT17 = 131072;
    private static final int BIT18 = 262144;

    private static final Map<Event, Integer> CLIP_CODES;
    private static final Map<Event, Integer> BUTTON_CODES;
    private static final Map<Event, Integer> MENU_CODES;

    private static final Map<Integer, Event> CLIP_EVENTS;
    private static final Map<Integer, Event> BUTTON_EVENTS;
    private static final Map<Integer, Event> MENU_EVENTS;

    static {
        CLIP_CODES = new LinkedHashMap<Event, Integer>();
        CLIP_CODES.put(Event.LOAD, BIT0);
        CLIP_CODES.put(Event.ENTER_FRAME, BIT1);
        CLIP_CODES.put(Event.UNLOAD, BIT2);
        CLIP_CODES.put(Event.MOUSE_MOVE, BIT3);
        CLIP_CODES.put(Event.MOUSE_DOWN, BIT4);
        CLIP_CODES.put(Event.MOUSE_UP, BIT5);
        CLIP_CODES.put(Event.KEY_DOWN, BIT6);
        CLIP_CODES.put(Event.KEY_UP, BIT7);
        CLIP_CODES.put(Event.DATA, BIT8);
        CLIP_CODES.put(Event.INITIALIZE, BIT9);
        CLIP_CODES.put(Event.PRESS, BIT10);
        CLIP_CODES.put(Event.RELEASE, BIT11);
        CLIP_CODES.put(Event.RELEASE_OUT, BIT12);
        CLIP_CODES.put(Event.ROLL_OVER, BIT13);
        CLIP_CODES.put(Event.ROLL_OUT, BIT14);
        CLIP_CODES.put(Event.DRAG_OVER, BIT15);
        CLIP_CODES.put(Event.DRAG_OUT, BIT16);
        CLIP_CODES.put(Event.KEY_PRESS, BIT17);
        CLIP_CODES.put(Event.CONSTRUCT, BIT18);

        CLIP_EVENTS = new LinkedHashMap<Integer, Event>();
        CLIP_EVENTS.put(BIT0, Event.LOAD);
        CLIP_EVENTS.put(BIT1, Event.ENTER_FRAME);
        CLIP_EVENTS.put(BIT2, Event.UNLOAD);
        CLIP_EVENTS.put(BIT3, Event.MOUSE_MOVE);
        CLIP_EVENTS.put(BIT4, Event.MOUSE_DOWN);
        CLIP_EVENTS.put(BIT5, Event.MOUSE_UP);
        CLIP_EVENTS.put(BIT6, Event.KEY_DOWN);
        CLIP_EVENTS.put(BIT7, Event.KEY_UP);
        CLIP_EVENTS.put(BIT8, Event.DATA);
        CLIP_EVENTS.put(BIT9, Event.INITIALIZE);
        CLIP_EVENTS.put(BIT10, Event.PRESS);
        CLIP_EVENTS.put(BIT11, Event.RELEASE);
        CLIP_EVENTS.put(BIT12, Event.RELEASE_OUT);
        CLIP_EVENTS.put(BIT13, Event.ROLL_OVER);
        CLIP_EVENTS.put(BIT14, Event.ROLL_OUT);
        CLIP_EVENTS.put(BIT15, Event.DRAG_OVER);
        CLIP_EVENTS.put(BIT16, Event.DRAG_OUT);
        CLIP_EVENTS.put(BIT17, Event.KEY_PRESS);
        CLIP_EVENTS.put(BIT18, Event.CONSTRUCT);

        BUTTON_CODES = new LinkedHashMap<Event, Integer>();
        BUTTON_CODES.put(Event.ROLL_OVER, BIT0);
        BUTTON_CODES.put(Event.ROLL_OUT, BIT1);
        BUTTON_CODES.put(Event.PRESS, BIT2);
        BUTTON_CODES.put(Event.RELEASE, BIT3);
        BUTTON_CODES.put(Event.DRAG_OUT, BIT4);
        BUTTON_CODES.put(Event.DRAG_OVER, BIT5);
        BUTTON_CODES.put(Event.RELEASE_OUT, BIT6);

        BUTTON_EVENTS = new LinkedHashMap<Integer, Event>();
        BUTTON_EVENTS.put(BIT0, Event.ROLL_OVER);
        BUTTON_EVENTS.put(BIT1, Event.ROLL_OUT);
        BUTTON_EVENTS.put(BIT2, Event.PRESS);
        BUTTON_EVENTS.put(BIT3, Event.RELEASE);
        BUTTON_EVENTS.put(BIT4, Event.DRAG_OUT);
        BUTTON_EVENTS.put(BIT5, Event.DRAG_OVER);
        BUTTON_EVENTS.put(BIT6, Event.RELEASE_OUT);

        MENU_CODES = new LinkedHashMap<Event, Integer>();
        MENU_CODES.put(Event.ROLL_OVER, BIT0);
        MENU_CODES.put(Event.ROLL_OUT, BIT1);
        MENU_CODES.put(Event.PRESS, BIT2);
        MENU_CODES.put(Event.RELEASE, BIT3);
        MENU_CODES.put(Event.RELEASE_OUT, BIT4);
        MENU_CODES.put(Event.DRAG_OVER, BIT7);
        MENU_CODES.put(Event.DRAG_OUT, BIT8);

        MENU_EVENTS = new LinkedHashMap<Integer, Event>();
        MENU_EVENTS.put(BIT0, Event.ROLL_OVER);
        MENU_EVENTS.put(BIT1, Event.ROLL_OUT);
        MENU_EVENTS.put(BIT2, Event.PRESS);
        MENU_EVENTS.put(BIT3, Event.RELEASE);
        MENU_EVENTS.put(BIT4, Event.RELEASE_OUT);
        MENU_EVENTS.put(BIT7, Event.DRAG_OVER);
        MENU_EVENTS.put(BIT8, Event.DRAG_OUT);
    }

    /** The events that the handler responds to. */
    private Set<Event> events;
    /** The code representing keyboard shortcut for the handler. */
    private int key;
    /** The actions executed by the handler when the event occurs. */
    private List<Action> actions;

    private int eventCode;
    private int length;
    private int offset;

    /**
     * Creates and initialises a EventHandler object using values
     * encoded in the Flash binary format.
     *
     * @param value
     *            is decoded by and it is dependent on the parent object. If
     *            it is a Place2 or Place3 object then the event handler is for
     *            a movie clip and the value represents the the set of events
     *            that the handler responds to. If the parent object is a
     *            button then the value is the length in bytes of the encoded
     *            actions executed by the handler.
     *
     * @param coder
     *            an SWFDecoder object that contains the encoded Flash data.
     *
     * @param context
     *            a Context object used to manage the decoders for different
     *            type of object and to pass information on how objects are
     *            decoded.
     *
     * @throws IOException
     *             if an error occurs while decoding the data.
     */
    public EventHandler(final int value, final SWFDecoder coder,
            final Context context) throws IOException {

        events = EnumSet.noneOf(Event.class);

        final int mask = 1;
        int field;

        if (context.contains(Context.TYPE)
                && context.get(Context.TYPE) == MovieTypes.DEFINE_BUTTON_2) {
            length = value;
            int eventKey = coder.readUnsignedShort();
            eventCode = eventKey & EVENT_MASK;
            key = (eventKey & KEY_MASK) >> KEY_OFFSET;

            if (context.contains(Context.MENU_BUTTON)) {
                for (int i = 0; i < NUM_BUTTON_EVENTS; i++) {
                    field = eventCode & (mask << i);
                    if (MENU_EVENTS.containsKey(field)) {
                        events.add(MENU_EVENTS.get(field));
                    }
                }
            } else {
                for (int i = 0; i < NUM_BUTTON_EVENTS; i++) {
                    field = eventCode & (mask << i);
                    if (field != 0 && BUTTON_EVENTS.containsKey(field)) {
                        events.add(BUTTON_EVENTS.get(field));
                    }
                }
            }
         } else {
            eventCode = value;
            length = coder.readInt();
            if ((eventCode & CLIP_CODES.get(Event.KEY_PRESS)) != 0) {
                key = coder.readByte();
                length -= 1;
            }
            for (int i = 0; i < NUM_CLIP_EVENTS; i++) {
                field = eventCode & (mask << i);
                if (field != 0 && CLIP_EVENTS.containsKey(field)) {
                    events.add(CLIP_EVENTS.get(field));
                }
            }
        }

        actions = new ArrayList<Action>();

        final SWFFactory<Action> decoder = context.getRegistry()
                .getActionDecoder();

        if (decoder == null) {
            if (length != 0) {
                actions.add(new ActionData(coder.readBytes(new byte[length])));
            }
        } else {
            coder.mark();
            while (coder.bytesRead() < length) {
                actions.add(decoder.getObject(coder, context));
            }
            coder.unmark();
        }
    }

    /**
     * Creates a ClipEvent object that with an array of actions that will be
     * executed when a particular event occurs.
     *
     * @param event
     *            the set of Events that the handler will respond to.
     * @param anArray
     *            the array of actions that will be executed when the specified
     *            event occurs.
     */
    public EventHandler(final Set<Event> event, final List<Action> anArray) {
        setEvents(event);
        setActions(anArray);
    }

    /**
     * Creates an EventHandler object that defines the array of actions that
     * will be executed when a particular event occurs or when the specified
     * key is pressed.
     *
     * @param event
     *            the set of Events that the handler will respond to.
     * @param character
     *            the ASCII code for the key pressed on the keyboard.
     * @param anArray
     *            the array of actions that will be executed when the specified
     *            event occurs. Must not be null.
     */
    public EventHandler(final Set<Event> event,
            final int character, final List<Action> anArray) {
        setEvents(event);
        setKey(character);
        setActions(anArray);
    }

    /**
     * Creates and initialises a EventHandler object using the values
     * copied from another EventHandler object.
     *
     * @param object
     *            a EventHandler object from which the values will be
     *            copied.
     */
    public EventHandler(final EventHandler object) {
        events = object.events;
        key = object.key;
        actions = new ArrayList<Action>(object.actions);
    }

    /**
     * Get the value that is encoded to represent the set of events that the
     * handler responds to.
     *
     * NOTE: This method is only used by Place2 and Place3 objects to encode
     * EventHandlers for movie clips. It should not be used.
     *
     * @return the value representing the set of encoded events.
     */
    public int getEventCode() {
        return eventCode;
    }

    /**
     * Get the set of events that the handler responds to.
     * @return a set of Events.
     */
    public Set<Event> getEvents() {
        return events;
    }

    /**
     * Set the events that the handler responds to.
     * @param set the set of Events for the handler.
     */
    public void setEvents(final Set<Event> set) {
        events = set;
    }

    /**
     * Get the code for the key that triggers the event when pressed. The
     * code is typically the ASCII code for standard western keyboards.
     *
     * @return the ASCII code for the key that triggers the event.
     */
    public int getKey() {
        return key;
    }

    /**
     * Sets the code for the key that triggers the event when pressed. The code
     * is typically the ASCII code for standard western keyboards.
     *
     * @param code
     *            the ASCII code for the key that triggers the event.
     */
    public void setKey(final int code) {
        key = code;
    }

    /**
     * Get the array of actions that are executed by the movie clip.
     *
     * @return the actions executed by the handler.
     */
    public List<Action> getActions() {
        return actions;
    }

    /**
     * Sets the array of actions that are executed by the handler in response
     * to specified event(s).
     *
     * @param array
     *            the array of actions that will be executed when the specified
     *            event occurs. Must not be null.
     */
    public void setActions(final List<Action> array) {
        if (array == null) {
            throw new IllegalArgumentException();
        }
        actions = array;
    }

    /**
     * Adds an action to the array of actions.
     *
     * @param anAction
     *            an action object. Must not be null.
     * @return this object.
     */
    public EventHandler add(final Action anAction) {
        if (anAction == null) {
            throw new IllegalArgumentException();
        }
        actions.add(anAction);
        return this;
    }

    /** {@inheritDoc} */
    public EventHandler copy() {
        return new EventHandler(this);
    }

    @Override
    public String toString() {
        return String.format(FORMAT, events, key, actions);
    }

    /** {@inheritDoc} */
    public int prepareToEncode(final Context context) {
        //CHECKSTYLE:OFF
        eventCode = 0;

        if (context.contains(Context.TYPE)
                && context.get(Context.TYPE) == MovieTypes.DEFINE_BUTTON_2) {
            if (context.contains(Context.MENU_BUTTON)) {
                for (Event event : events) {
                    eventCode |= MENU_CODES.get(event);
                }
            } else {
                for (Event event : events) {
                    eventCode |= BUTTON_CODES.get(event);
                }
            }

            length = 4;
            for (final Action action : actions) {
                length += action.prepareToEncode(context);
            }
            if (context.contains(Context.LAST)) {
                offset = -2;
            } else {
                offset = length - 2;
            }
        } else {
            for (Event event : events) {
                eventCode |= CLIP_CODES.get(event);
            }

            if (context.get(Context.VERSION) >= VERSION_WITH_EXT_EVENTS) {
                length = 8;
            } else {
                length = 6;
            }
            offset = (eventCode & CLIP_CODES.get(Event.KEY_PRESS)) == 0 ? 0 : 1;

            for (final Action action : actions) {
                offset += action.prepareToEncode(context);
            }

            length += offset;
        }
        return length;
        //CHECKSTYLE:ON
    }

    /** {@inheritDoc} */
    public void encode(final SWFEncoder coder, final Context context)
            throws IOException {
        coder.mark();
        if (context.contains(Context.TYPE)
                && context.get(Context.TYPE) == MovieTypes.DEFINE_BUTTON_2) {
            coder.writeShort(offset + 2);
            coder.writeShort((key << KEY_OFFSET) | eventCode);
        } else {
            if (context.get(Context.VERSION) >= VERSION_WITH_EXT_EVENTS) {
                coder.writeInt(eventCode);
            } else {
                coder.writeShort(eventCode);
            }

            coder.writeInt(offset);

            if ((eventCode & CLIP_CODES.get(Event.KEY_PRESS)) != 0) {
                coder.writeByte(key);
            }
        }

        for (final Action action : actions) {
            action.encode(coder, context);
        }
        coder.unmark(length);
    }
}
