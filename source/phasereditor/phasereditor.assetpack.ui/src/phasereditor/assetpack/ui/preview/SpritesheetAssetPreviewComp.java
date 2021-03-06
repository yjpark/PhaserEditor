// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.assetpack.ui.preview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ListDialog;

import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel.FrameModel;
import phasereditor.assetpack.ui.widgets.SpritesheetPreviewCanvas;
import phasereditor.canvas.core.AnimationModel;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.ImageCanvas;
import phasereditor.ui.ImageCanvas_Zoom_1_1_Action;
import phasereditor.ui.ImageCanvas_Zoom_FitWindow_Action;
import phasereditor.ui.animations.FrameAnimationCanvas;

public class SpritesheetAssetPreviewComp extends Composite {

	SpritesheetPreviewCanvas _sheetCanvas;

	protected List<FrameModel> _selectedFrames;

	public SpritesheetAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		setLayout(new StackLayout());

		_sheetCanvas = new SpritesheetPreviewCanvas(this, SWT.NONE);

		DragSource dragSource = new DragSource(_sheetCanvas, DND.DROP_MOVE | DND.DROP_DEFAULT);
		dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
		dragSource.addDragListener(new DragSourceAdapter() {

			@Override
			public void dragStart(DragSourceEvent event) {
				ISelection sel = getSelection();
				if (sel.isEmpty()) {
					event.doit = false;
					return;
				}
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				transfer.setSelection(sel);
			}

			private ISelection getSelection() {
				if (_selectedFrames == null) {
					return StructuredSelection.EMPTY;
				}

				return new StructuredSelection(_selectedFrames);
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (_selectedFrames != null && !_selectedFrames.isEmpty()) {
					event.data = _selectedFrames.get(0).getName();
				}
			}
		});

		_animCanvas = new FrameAnimationCanvas(this, SWT.NONE);

		afterCreateWidgets();

	}

	private void afterCreateWidgets() {

		// DnD

		class Listener extends MouseAdapter {

			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == 1) {
					_selectedFrames = _sheetCanvas.getSelectedFrames();
				}
			}
		}

		_sheetCanvas.addMouseListener(new Listener());
	}

	private SpritesheetAssetModel _model;

	private SpritesheetAnimationModel _animModel;

	protected void playButtonPressed() {
		StackLayout layout = (StackLayout) getLayout();

		if (isSheetInTheTop()) {
			layout.topControl = _animCanvas;
			_animCanvas.stop();
			_animCanvas.play();
		} else {
			layout.topControl = _sheetCanvas;
			_animCanvas.stop();
		}

		layout();

		// boolean single = _canvas.isSingleFrame();
		// single = !single;
		// _animation.pause(!single);
		// _canvas.setSingleFrame(single);
		// _canvas.fitWindow();
		// _canvas.redraw();

	}

	private boolean isSheetInTheTop() {
		StackLayout layout = (StackLayout) getLayout();
		return layout.topControl == _sheetCanvas;
	}

	public void setModel(SpritesheetAssetModel model) {
		_model = model;

		_selectedFrames = new ArrayList<>();

		IFile imgFile = model.getUrlFile();

		{
			// sprite canvas
			_sheetCanvas.setSpritesheet(model);

			_sheetCanvas.setImageFile(imgFile);

			String str = "Frames Size: " + model.getFrameWidth() + "x" + model.getFrameHeight();
			if (_sheetCanvas.getImage() != null) {
				str += "\n";
				Rectangle b = _sheetCanvas.getImage().getBounds();
				str += "Image Size: " + b.width + "x" + b.height + "\n";
				str += "Image URL: " + model.getUrl();
			}
			_sheetCanvas.setToolTipText(str);
		}

		{
			// anim canvas
			AnimationModel anim = new AnimationModel("noname");
			anim.getFrames().addAll(_model.getFrames());
			anim.setFrameRate(5);
			anim.setLoop(true);
			_animModel = new SpritesheetAnimationModel(anim);
			_animCanvas.setModel(_animModel);
			_animCanvas.stop();
		}

		((StackLayout) getLayout()).topControl = _sheetCanvas;
		layout();
	}

	public SpritesheetAssetModel getModel() {
		return _model;
	}

	public void setFps(int fps) {
		_animModel.setFrameRates(fps);
		if (!isSheetInTheTop()) {
			_animCanvas.play();
		}
	}

	private Action _playAction;
	private FrameAnimationCanvas _animCanvas;

	private Action _setFpsAction;

	public void createToolBar(IToolBarManager toolbar) {

		// play buttons

		_playAction = new Action("Play") {
			@Override
			public void run() {
				boolean b = getText().equals("Play");
				setText(b ? "Stop" : "Play");
				setImageDescriptor(b ? EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_STOP)
						: EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PLAY));
				playButtonPressed();
			}
		};
		_playAction.setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_PLAY));
		toolbar.add(_playAction);

		// settings

		Object[] fpsList = new Object[6 + 2];
		for (int i = 0; i < 6; i++) {
			fpsList[i + 2] = Integer.valueOf((i + 1) * 10);
		}
		fpsList[0] = Integer.valueOf(1);
		fpsList[1] = Integer.valueOf(5);

		_setFpsAction = new Action("FPS") {

			{
				setImageDescriptor(EditorSharedImages.getImageDescriptor(IEditorSharedImages.IMG_CONTROL_EQUALIZER));
			}

			@SuppressWarnings({ "boxing", "synthetic-access" })
			@Override
			public void run() {
				ListDialog dlg = new ListDialog(getShell());
				dlg.setContentProvider(new ArrayContentProvider());
				dlg.setLabelProvider(new LabelProvider());
				dlg.setInput(fpsList);
				dlg.setInitialSelections(new Object[] { _animModel.getFrameRate() });
				dlg.setMessage("Select the frames per second:");
				dlg.setTitle("FPS");

				if (dlg.open() == Window.OK) {
					Integer fps = (Integer) dlg.getResult()[0];
					setFps(fps.intValue());
				}
			}
		};
		toolbar.add(_setFpsAction);

		toolbar.add(new Separator());
		toolbar.add(new ImageCanvas_Zoom_1_1_Action() {

			@Override
			public ImageCanvas getImageCanvas() {
				return (ImageCanvas) ((StackLayout) getLayout()).topControl;
			}
		});
		toolbar.add(new ImageCanvas_Zoom_FitWindow_Action() {

			@Override
			public ImageCanvas getImageCanvas() {
				return (ImageCanvas) ((StackLayout) getLayout()).topControl;
			}
		});

	}

	public void stopAnimation() {
		_animCanvas.stop();
	}
}
