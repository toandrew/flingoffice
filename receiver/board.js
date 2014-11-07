/**
 * Creates an empty board object with no location
 * @param {CanvasRenderingContext2D} context the 2D context of the canvas that
 *     the board is drawn on.
 * @constructor
 */
function board(context) {
    this.mContext = context;

    this.pdfDoc = null;
    this.pageNum = 1;
    this.scale = 1;
    //this.scale = 2;

    this.pageRendering = false,
    this.pageNumPending = null,

    this.mContext.canvas.width  = window.innerWidth; 
    this.mContext.canvas.height = window.innerHeight;
}

/**
 * Resets the board to a starting state.
 * @this {board}
 */

function loadPdf(source) {
    //
    // Asynchronously download PDF as an ArrayBuffer
    //
    var that = this; 
    console.log("!in loadPdf:" + source);
    PDFJS.getDocument(source).then(function getPdfHelloWorld(_pdfDoc) {
        that.pdfDoc = _pdfDoc;
        that.renderPage(that, that.pageNum);
    });
}

//
// Go to previous page
//
function goPrevious() {
    if (this.pageNum <= 1) {
        return;
    }
    this.pageNum--;
    this.queueRenderPage(this.pageNum);
}

//
// Go to next page
//
function goNext() {
    if (this.pageNum >= this.pdfDoc.numPages) {
        return;
    }

    this.pageNum++;
    this.queueRenderPage(this.pageNum);
}

//
// render one page.
//
function renderPage(t, num) {
    console.log("reanderPage:" + t.num);

    var that = t;

    that.pageRendering = true;

    // Using promise to fetch the page
    this.pdfDoc.getPage(num).then(function(page) {
        var viewport = page.getViewport(that.scale);
        that.mContext.canvas.height = viewport.height;
        that.mContext.canvas.width = viewport.width;

        // Render PDF page into canvas context
        var renderContext = {
            canvasContext: that.mContext,
            viewport: viewport
        };

        var renderTask = page.render(renderContext);

        // Wait for rendering to finish
        renderTask.promise.then(function () {
           console.log("render done?!");
           that.pageRendering = false;
           if (that.pageNumPending !== null) {
              // New page rendering is pending
              that.renderPage(that, that.pageNumPending);
              that.pageNumPending = null;
           }
        });
    });
}

/**
 * If another page rendering in progress, waits until the rendering is
 * finised. Otherwise, executes rendering immediately.
*/
function queueRenderPage(num) {
    console.log("in queueRenderPage!" + num);
    if (this.pageRendering) {
      console.log("queue render!");
      this.pageNumPending = num;
    } else {
      this.renderPage(this, num);
    }
}

board.prototype.loadPdf = loadPdf;
board.prototype.renderPage = renderPage;
board.prototype.goPrevious = goPrevious;
board.prototype.goNext = goNext;
board.prototype.queueRenderPage = queueRenderPage;
