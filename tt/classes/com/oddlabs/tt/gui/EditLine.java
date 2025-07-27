package com.oddlabs.tt.gui;

import com.oddlabs.tt.event.LocalEventQueue;
import java.util.ArrayList;
import java.util.List;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import org.lwjgl.input.Keyboard;
import org.lwjgl.Sys;
import com.oddlabs.tt.font.Index;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.tt.guievent.EnterListener;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public strictfp class EditLine extends TextField {

    public final static int RIGHT_ALIGNED = 1;
    public final static int LEFT_ALIGNED = 2;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private final List enter_listeners = new ArrayList();
    private final int alignment;
    private final String allowed_chars;
    private final int max_text_width;

    private TextLineRenderer text_renderer;
    private int offset_x;
    private int index;

    public EditLine(int width, int max_chars) {
        this(width, max_chars, LEFT_ALIGNED);
    }

    public EditLine(int width, int max_chars, int alignment) {
        this(width, max_chars, null, alignment);
    }

    public EditLine(int width, int max_chars, String allowed_chars, int alignment) {
        super(Skin.getSkin().getEditFont(), max_chars);
        this.allowed_chars = allowed_chars;
        this.alignment = alignment;
        Box edit_box = Skin.getSkin().getEditBox();
        setDim(width, getFont().getHeight() + edit_box.getBottomOffset() + edit_box.getTopOffset());
        setCanFocus(true);
        text_renderer = new TextLineRenderer(getFont());
        this.max_text_width = width - edit_box.getLeftOffset() - edit_box.getRightOffset();
        clear();
    }

    protected final int getCursorIndex() {
        return GUIRoot.CURSOR_TEXT;
    }

    protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        Box edit_box = Skin.getSkin().getEditBox();
        if (isDisabled()) {
            edit_box.render(0, 0, getWidth(), getHeight(), Skin.DISABLED);
        } else {
            edit_box.render(0, 0, getWidth(), getHeight(), Skin.NORMAL);
        }
        int render_index = isActive() ? index : -1;
        renderText(text_renderer, edit_box.getLeftOffset(), edit_box.getBottomOffset(), offset_x, clip_left, clip_right, clip_bottom, clip_top, render_index);
    }

    protected void renderText(TextLineRenderer text_renderer, int x, int y, int offset_x, float clip_left, float clip_right, float clip_bottom, float clip_top, int render_index) {
        clip_left = StrictMath.max(clip_left, x);
        clip_right = StrictMath.min(clip_right, x + max_text_width);
        text_renderer.render(x, y, offset_x, clip_left, clip_right, clip_bottom, clip_top, getText(), render_index);
        renderHighlight(text_renderer, x, y, offset_x, clip_left, clip_right, clip_bottom, clip_top);
    }

    /**
     * Renders the text highlight for the selected text by using the
     * selectionStart and selectionEnd values.
     *
     * @param text_renderer The text line renderer - to calculate the highlight
     * positions.
     * @param x The x position.
     * @param y The y position.
     * @param offset_x The x offset for highlight to start in the box
     */
    private void renderHighlight(TextLineRenderer text_renderer, int x, int y, int offset_x, float clip_left, float clip_right, float clip_bottom, float clip_top) {
        if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            int selStartX = text_renderer.getIndexRenderX(x, y, offset_x, getText(), selectionStart);
            int selEndX = text_renderer.getIndexRenderX(x, y, offset_x, getText(), selectionEnd);
            int highlightLeft = Math.min(selStartX, selEndX);
            int highlightRight = Math.max(selStartX, selEndX);
            Skin.getSkin().getEditBox().renderHighlight(highlightLeft, y, highlightRight - highlightLeft, getFont().getHeight(), clip_left, clip_right, clip_bottom, clip_top);
        }
    }

    @Override
    protected void keyReleased(KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case Keyboard.KEY_RETURN:
                enterPressedAll();
                break;
            default:
                super.keyReleased(event);
                break;
        }
    }

    @Override
    protected void keyRepeat(KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case Keyboard.KEY_BACK:
                // Selection deletion is handled in doShiftModifier()
                if (index > 0 && selectionStart == -1 && selectionEnd == -1) {
                    index--;
                    if (alignment == RIGHT_ALIGNED) {
                        char key = getText().charAt(index);
                        offset_x += (getFont().getQuad(key).getWidth() - getFont().getXBorder());
                    }
                    delete(index);
                }
                break;
            case Keyboard.KEY_DELETE:
                // Selection deletion is handled in doShiftModifier()
                if (index < getText().length() && selectionStart == -1 && selectionEnd == -1) {
                    if (alignment == RIGHT_ALIGNED) {
                        char key = getText().charAt(index);
                        offset_x += (getFont().getQuad(key).getWidth() - getFont().getXBorder());
                    }
                    delete(index);
                }
                break;
            case Keyboard.KEY_LEFT:
                if (index > 0) {
                    index--;
                }
                break;
            case Keyboard.KEY_RIGHT:
                if (index < getText().length()) {
                    index++;
                }
                break;
            case Keyboard.KEY_HOME:
                // Selection home function is handled in doShiftModifier()
                if (!LocalInput.isShiftDownCurrently()) {
                    index = 0;
                    selectionStart = -1;
                    selectionEnd = -1;
                }
                break;
            case Keyboard.KEY_END:
                // Selection end function is handled in doShiftModifier()
                if (!LocalInput.isShiftDownCurrently()) {
                    index = getText().length();
                    selectionStart = -1;
                    selectionEnd = -1;
                }
                break;
            case Keyboard.KEY_TAB:
            case Keyboard.KEY_RETURN:
                super.keyRepeat(event);
                break;
            case Keyboard.KEY_ESCAPE:
                selectionStart = -1;
                selectionEnd = -1;
                break;
            default:
                char key = event.getKeyChar();
                if (isAllowed(key)) {
                    boolean result = insert(index, key);
                    assert result;
                    index++;
                    if (alignment == RIGHT_ALIGNED) {
                        offset_x -= (getFont().getQuad(key).getWidth() - getFont().getXBorder());
                    }
                } else {
                    super.keyRepeat(event);
                }
                break;
        }
        if (doControlModifier(event)) {
            correctOffsetX();
            return;
        } // Highlight text
        else if (doShiftModifier(event)) {
            correctOffsetX();
            return;
        }
        correctOffsetX();
    }

    public final boolean isAllowed(char ch) {
        return super.isAllowed(ch) && getFont().getQuad(ch) != null && (allowed_chars == null || allowed_chars.indexOf(ch) != -1);
    }

    private final void correctOffsetX() {
        Box edit_box = Skin.getSkin().getEditBox();
        int index_render_x = text_renderer.getIndexRenderX(edit_box.getLeftOffset(),
                edit_box.getBottomOffset(),
                offset_x,
                getText(),
                index);

        int max_x = computeMaxX();
        if (index_render_x > max_x) {
            offset_x -= index_render_x - max_x;
        } else if (index_render_x < edit_box.getLeftOffset()) {
            offset_x += edit_box.getLeftOffset() - index_render_x;
        }
    }

    private final int computeMaxX() {
        Box edit_box = Skin.getSkin().getEditBox();
        return getWidth() - edit_box.getRightOffset() - Index.INDEX_WIDTH/* - getFont().getXBorder()/2*/;
    }

    public final int getIndex() {
        return index;
    }

    /**
     * Sets the index aka the cursor position to the given value
     */
    public final void setIndex(int index) {
        this.index = index;
    }

    public final void clear() {
        super.clear();
        index = 0;
        if (alignment == LEFT_ALIGNED) {
            offset_x = -getFont().getXBorder() / 2;
        } else {
            int text_width = Skin.getSkin().getEditFont().getWidth(getText());
            offset_x = max_text_width - text_width - Index.INDEX_WIDTH - getFont().getXBorder() / 2;
        }
    }

    protected final void appendNotify(CharSequence str) {
        if (alignment == RIGHT_ALIGNED) {
            for (int i = 0; i < str.length(); i++) {
                char key = str.charAt(i);
                offset_x -= (getFont().getQuad(key).getWidth() - getFont().getXBorder());
            }
        }
    }

    protected void focusNotify(boolean focus) {
        if (focus) {
            index = getText().length();
        }
    }

    protected final void mouseEntered() {
    }

    protected final void mouseExited() {
    }

    protected final void mousePressed(int button, int x, int y) {
        if (button == LocalInput.LEFT_BUTTON) {
            if (this.selectionStart != -1 && this.selectionEnd != -1) {
                this.selectionStart = -1;
                this.selectionEnd = -1;
            }

            Box edit_box = Skin.getSkin().getEditBox();
            index = text_renderer.jumpDirect(edit_box.getLeftOffset() + offset_x,
                    edit_box.getBottomOffset(),
                    x,
                    getText(),
                    index);
        }
    }

    public final void enterPressedAll() {
        CharSequence text = getText();
        enterPressed(text);
        for (int i = 0; i < enter_listeners.size(); i++) {
            EnterListener listener = (EnterListener) enter_listeners.get(i);
            if (listener != null) {
                listener.enterPressed(text);
            }
        }
    }

    protected void enterPressed(CharSequence text) {
    }

    public final void addEnterListener(EnterListener listener) {
        enter_listeners.add(listener);
    }

    private boolean doControlModifier(KeyboardEvent event) {
        if (LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_V) {
            pasteClipboard();
            return true;
        } else if (LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_C) {
            copySelectionToClipboard();
            return true;
        } else if (LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_X) {
            cutSelectionToClipboard();
            return true;
        } else if (LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_A) {
            selectionStart = 0;
            selectionEnd = getContents().length();
            setIndex(selectionEnd);
            correctOffsetX();
            return true;
        } else if (!LocalInput.isShiftDownCurrently() && LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_LEFT) {
            String contents = getContents();
            if (getIndex() == 0) {
                return true;
            }
            boolean isCursorAtStartOfSelection = getIndex() <= selectionStart || selectionStart == -1;
            int startingIndex = getIndex();

            // When cursor is not at the start of the word we will start at the current word for the next selection
            if (!isCursorAtStartOfSelection) {
                startingIndex = selectionStart;
            }

            boolean characterBehindCursorIsSpace = contents.charAt(getIndex()) == ' ';

            if (characterBehindCursorIsSpace) {
                while (startingIndex > 0 && contents.charAt(startingIndex) == ' ') {
                    startingIndex--;
                }
            }

            boolean didLoop = false;
            while (startingIndex >= 0 && contents.charAt(startingIndex) != ' ') {
                didLoop = true;
                startingIndex--;
            }
            // set the cursor back one since we went past the space
            if (didLoop) {
                startingIndex++;
            }

            setIndex(startingIndex);
            if (selectionStart != -1 || selectionEnd != -1) {
                // If we have a selection, we need to clear it
                selectionStart = -1;
                selectionEnd = -1;
            }
        } else if (!LocalInput.isShiftDownCurrently() && LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_RIGHT) {
            String contents = getContents();
            if (getIndex() == contents.length()) {
                return true;
            }

            boolean isCursorAtStartOfSelection = getIndex() <= selectionStart || selectionStart == -1;
            int startingIndex = getIndex() - 1;
            if (!isCursorAtStartOfSelection) {
                startingIndex = selectionEnd;
            }

            boolean characterInfrontOfCursorIsSpace = contents.charAt(getIndex() - 1) == ' ';
            if (characterInfrontOfCursorIsSpace) {

                while (startingIndex < contents.length() && contents.charAt(startingIndex) == ' ') {
                    startingIndex++;
                }
            }

            boolean didLoop = false;
            while (startingIndex < contents.length() && contents.charAt(startingIndex) != ' ') {
                startingIndex++;
                didLoop = true;
            }

            // set the cursor back one since we went past the next space that was there
            if (didLoop) {
                startingIndex--;
            }

            setIndex(startingIndex + 1);
            if (selectionStart != -1 || selectionEnd != -1) {
                // If we have a selection, we need to clear it
                selectionStart = -1;
                selectionEnd = -1;
            }
            return true;
        }
        // TODO: DO CUT TO CLIPBOARD?

        return false;
    }

    private void pasteClipboard() {
        String clipboard = (String) LocalEventQueue.getQueue().getDeterministic().log(Sys.getClipboard());
        if (clipboard != null) {
            for (char item : clipboard.toCharArray()) {
                if (!isAllowed(item)) {
                    return;
                }
            }

            if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
                // delete the selected text first
                String beforeReplace = getContents().substring(0, selectionStart);
                String afterReplace = getContents().substring(beforeReplace.length() + (selectionEnd - selectionStart), getContents().length());
                String newContent = beforeReplace + clipboard + afterReplace;
                if (newContent.length() > max_chars) {
                    return;
                }
                set(newContent);
                // Set cursor after the pasted content
                setIndex(beforeReplace.length() + clipboard.length());
                // Clear selection
                selectionStart = -1;
                selectionEnd = -1;
            } else {
                String beforeCursorString = getContents().substring(0, getIndex());
                String afterCursorString = getContents().substring(getIndex());
                String newContent = beforeCursorString + clipboard + afterCursorString;
                if (newContent.length() > max_chars) {
                    return;
                }
                set(newContent);
                setIndex(beforeCursorString.length() + clipboard.length());
            }
        }
    }

    private void copySelectionToClipboard() {
        if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            String selectedText = getContents().substring(selectionStart, selectionEnd);

            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(selectedText);
            clipboard.setContents(selection, null);
        }
    }

    private void cutSelectionToClipboard() {
        if (selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            String selectedText = getContents().substring(selectionStart, selectionEnd);
            
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection selection = new StringSelection(selectedText);
            clipboard.setContents(selection, null);

            // Remove the selected text from the EditLine
            String beforeReplace = getContents().substring(0, selectionStart);
            String afterReplace = getContents().substring(beforeReplace.length() + (selectionEnd - selectionStart), getContents().length());
            String newContent = beforeReplace + afterReplace;
            set(newContent);
            setIndex(beforeReplace.length());
            // Clear selection
            selectionStart = -1;
            selectionEnd = -1;
        }
    }

    private boolean doShiftModifier(KeyboardEvent event) {
        // TODO: Same but for ctrl
        if (LocalInput.isShiftDownCurrently() && LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_LEFT) {
            String contents = getContents();
            if (getIndex() == 0) {
                return true;
            }
            boolean isCursorAtStartOfSelection = getIndex() <= selectionStart || selectionStart == -1;
            int startingIndex = getIndex();

            // When cursor is not at the start of the word we will start at the current word for the next selection
            if (!isCursorAtStartOfSelection) {
                startingIndex = selectionStart;
            }

            boolean characterBehindCursorIsSpace = contents.charAt(getIndex()) == ' ';

            if (characterBehindCursorIsSpace) {
                while (startingIndex > 0 && contents.charAt(startingIndex) == ' ') {
                    startingIndex--;
                }
            }

            boolean didLoop = false;
            while (startingIndex >= 0 && contents.charAt(startingIndex) != ' ') {
                didLoop = true;
                startingIndex--;
            }
            // set the cursor back one since we went past the space
            if (didLoop) {
                startingIndex++;
            }

            if (selectionStart == -1) {
                selectionStart = startingIndex;
                selectionEnd = getIndex() + 1;
            } else {
                selectionStart = startingIndex;
            }
            setIndex(startingIndex);
        } else if (LocalInput.isShiftDownCurrently() && LocalInput.isControlDownCurrently() && event.getKeyCode() == Keyboard.KEY_RIGHT) {
            String contents = getContents();
            if (getIndex() == contents.length()) {
                return true;
            }

            boolean isCursorAtStartOfSelection = getIndex() <= selectionStart || selectionStart == -1;
            int startingIndex = getIndex() - 1;
            if (!isCursorAtStartOfSelection) {
                startingIndex = selectionEnd;
            }

            boolean characterInfrontOfCursorIsSpace = contents.charAt(getIndex() - 1) == ' ';
            if (characterInfrontOfCursorIsSpace) {

                while (startingIndex < contents.length() && contents.charAt(startingIndex) == ' ') {
                    startingIndex++;
                }
            }

            boolean didLoop = false;
            while (startingIndex < contents.length() && contents.charAt(startingIndex) != ' ') {
                startingIndex++;
                didLoop = true;
            }

            // set the cursor back one since we went past the next space that was there
            if (didLoop) {
                startingIndex--;
            }

            if (selectionStart == -1) {
                selectionStart = getIndex() - 1;
                selectionEnd = startingIndex + 1;
            } else {
                selectionEnd = startingIndex + 1;
            }

            setIndex(selectionEnd);
            return true;
        } else if (LocalInput.isShiftDownCurrently() && event.getKeyCode() == Keyboard.KEY_HOME) {
            boolean isCursorAtStartOfSelection = getIndex() <= selectionStart;
            if (selectionStart == -1) {
                selectionStart = getIndex();
                selectionEnd = getIndex();
            }

            if (!isCursorAtStartOfSelection) {
                selectionEnd = selectionStart;
            }
            selectionStart = 0;
            setIndex(0);
            return true;
        } else if (LocalInput.isShiftDownCurrently() && event.getKeyCode() == Keyboard.KEY_END) {
            boolean isCursorAtStartOfSelection = getIndex() <= selectionStart;
            if (selectionStart == -1) {
                selectionStart = getIndex();
                selectionEnd = getIndex();
            }

            if (isCursorAtStartOfSelection) {
                selectionStart = selectionEnd;
            }
            selectionEnd = getContents().length();
            setIndex(getContents().length());
            return true;
        } else if (LocalInput.isShiftDownCurrently() && event.getKeyCode() == Keyboard.KEY_RIGHT && (selectionEnd < getContents().length() || selectionEnd == -1)) {
            // First time shift is pressed and cursor has moved
            if (selectionStart == -1) {
                selectionStart = getIndex() - 1;
                selectionEnd = getIndex();
            } else {
                // when right is pressed the selection moves 1 to the right by the time we are evaluating this
                // other inputs are processed before mofifier keys at the moment
                if (getIndex() <= selectionStart + 1) {
                    selectionStart++;
                } else {
                    selectionEnd++;
                }
            }

            if (selectionStart >= getContents().length()) {
                selectionStart = getContents().length();
            }
            if (selectionEnd >= getContents().length()) {
                selectionEnd = getContents().length();
            }
            return true;
        } else if (LocalInput.isShiftDownCurrently() && event.getKeyCode() == Keyboard.KEY_LEFT && (selectionStart > 0 || selectionStart == -1)) {
            // First time shift is pressed and the cursor has moved
            if (selectionStart == -1) {
                selectionStart = getIndex();
                if (selectionStart < 0) {
                    selectionStart = 0;
                }
                selectionEnd = getIndex() + 1;
            } else {
                // cursor is at start of selection
                if (getIndex() < selectionStart) {
                    selectionStart--;
                } else {
                    selectionEnd--;
                }
            }

            if (selectionEnd < 0) {
                selectionEnd = 0;
            }
            if (selectionStart < 0) {
                selectionStart = 0;
            }
            return true;
        } else if (selectionStart != -1 && selectionEnd != -1
                && (event.getKeyChar() != '\0'
                || event.getKeyCode() == Keyboard.KEY_DELETE
                || event.getKeyCode() == Keyboard.KEY_BACK)) {
            String beforeReplace;
            if (event.getKeyCode() != Keyboard.KEY_BACK && event.getKeyCode() != Keyboard.KEY_DELETE) {
                beforeReplace = getContents().substring(0, selectionStart) + event.getKeyChar();
            } else {
                beforeReplace = getContents().substring(0, selectionStart);
            }
            String afterReplace = getContents().substring(beforeReplace.length() + (selectionEnd - selectionStart), getContents().length());
            String newContent = beforeReplace + afterReplace;

            // resets the cursor index to 0
            set(newContent);

            // indexing needs to be adjusted based on if a new key is typed or backspace/delete is hit
            if (event.getKeyCode() == Keyboard.KEY_BACK || event.getKeyCode() == Keyboard.KEY_DELETE) {
                // Sets the cursor after the replaced content
                if (newContent.length() == 0) {
                    setIndex(0);
                } else {

                    // If we're in the middle of the text, keep the cursor at the start of the replaced section
                    setIndex(beforeReplace.length());

                    if (index > newContent.length()) {
                        setIndex(newContent.length());
                    }
                }
            } else {
                setIndex(beforeReplace.length());
            }

            selectionStart = -1;
            selectionEnd = -1;
        } // unhighlight if the cursor moves without shift
        else if (!LocalInput.isShiftDownCurrently() && (event.getKeyCode() == Keyboard.KEY_LEFT || event.getKeyCode() == Keyboard.KEY_RIGHT)
                && (selectionStart != -1 || selectionEnd != -1)) {
            // Clear selection if shift is not down and the key pressed is not left/right
            if (event.getKeyCode() == Keyboard.KEY_LEFT) {
                setIndex(selectionStart);
            } else if (event.getKeyCode() == Keyboard.KEY_RIGHT) {
                setIndex(selectionEnd);
            }
            selectionStart = -1;
            selectionEnd = -1;
        }

        return false;
    }
}
