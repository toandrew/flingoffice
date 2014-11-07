from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import base64
import json
import os
from signal import SIGINT
import urlparse
class MyHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        try:
            self.send_response(404)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            self.wfile.write("404, it's a good day today ...")
        except IOError:
            self.send_error(404, 'File Not Found: %s' % self.path)

    def do_POST(self):
        try:
            content_len = int(self.headers.getheader('content-length', 0))
            if content_len > 0:
                self.send_response(200)
                self.send_header('Content-type', 'text/html')
                self.end_headers()

                print "Process JSON commands, please wait ..."
                tmp1 = self.rfile.read(content_len)
#                 tmp2 = tmp1.decode('utf-8')
                post_data = urlparse.parse_qs(tmp1)
                request = json.loads(post_data["request"][0])
                if "cmd" in request:
                    cmd = request["cmd"]
                    type = request["type"]
                    if (cmd != "view"):
                        return
                    self.wfile.write("{'result':0}")
                    print "OK"
                elif "filename" in request:
                    file_type = request["type"]
                    if file_type != "pdf":
                        filename = request["filenamemd5"]
                    else:
                        filename = request["filenamemd5"] + '.pdf'
                    md5 = request["md5"]
                    tmppath = '/tmp/' + md5
                    os.system('mkdir -p ' + tmppath)

                    print "make dir: " + tmppath

                    file = request["file"]
                    binfile = base64.b64decode(file)
                    f = open(tmppath + '/' + filename, 'wb')
                    try:
                        f.write(binfile)
                    finally:
                        f.close()

                    print "dump to file: " + tmppath + "/" + filename

                    result = "{'result':0, 'content':["

                    file_type = request["type"]
                    if file_type != "pdf":
                        os.system(
                            "cd " + tmppath + " && unoconv -f pdf '" + filename + "'")
                    pdffiles = os.popen('ls ' + tmppath + '/*.pdf')
                    pdffilenames = pdffiles.readlines()
                    count = 0
                    for pdffilename in pdffilenames:
                        pdffilename = pdffilename.split('\n')[0]
                        print "generate: " + pdffilename
                        pdffile = open(pdffilename, 'rb').read()
                        pdffileb64 = base64.encodestring(pdffile)
                        if count != 0:
                            result += ", "
                        result += "{'name':'" + pdffilename.rpartition(
                            '/')[2] + "', 'file':'" + pdffileb64 + "'}"
                        count += 1
                    result += "]}"

                    self.wfile.write(result)

                    os.system('rm -rf ' + tmppath)

                    print "OK"
        except Exception, e:
            print 'We got error:', e


def main():
    xvfbpid = -1
    sofficepid = -2

    #ret = os.fork()
    #if ret == 0:
    #    os.execvp('Xvfb', ['Xvfb', ':1'])
    #    return
    #else:
    #    xvfbpid = ret

    #ret = os.fork()
    #if ret == 0:
    #    os.system('sleep 3')
    #    os.execvp("soffice", ["soffice", "--norestore", "--display",
    #                          ":1.0", "--accept=socket,host=localhost,port=2002;urp;"])
    #    return
    #else:
    #    sofficepid = ret

    try:
        server = HTTPServer(('', 8765), MyHandler)
        print 'welcome to the ,machine...',
        print 'Press ^C once or twice to quit'
        server.serve_forever()
    except KeyboardInterrupt:
        print '^C received,shutting down server'
        server.socket.close()
     #   os.kill(xvfbpid, SIGINT)
     #   os.kill(sofficepid, SIGINT)

if __name__ == '__main__':
    main()
