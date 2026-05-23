import { Route, Routes } from "react-router-dom";
import { Home } from "@/pages/Home";
import { Track } from "@/pages/Track";
import { Summary } from "@/pages/Summary";
import { History } from "@/pages/History";

export default function App() {
  return (
    <div className="min-h-full flex justify-center bg-background">
      <div className="w-full max-w-[440px] min-h-screen px-5 py-6 flex flex-col">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/track" element={<Track />} />
          <Route path="/summary/:id" element={<Summary />} />
          <Route path="/history" element={<History />} />
        </Routes>
      </div>
    </div>
  );
}
