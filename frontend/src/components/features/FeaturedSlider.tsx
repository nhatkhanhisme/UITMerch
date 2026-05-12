import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';

export interface FeaturedItem {
  id: string;
  name: string;
  orgName?: string;
  desc?: string;
  image: string | null;
  link?: string;
}

interface FeaturedSliderProps {
  items: FeaturedItem[];
  autoPlayInterval?: number;
}

export function FeaturedSlider({ items, autoPlayInterval = 4000 }: FeaturedSliderProps) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const nextSlide = useCallback(() => {
    setCurrentIndex((prev) => (prev === items.length - 1 ? 0 : prev + 1));
  }, [items.length]);

  useEffect(() => {
    const timer = setInterval(nextSlide, autoPlayInterval);
    return () => clearInterval(timer);
  }, [nextSlide, autoPlayInterval]);

  if (!items?.length) return null;

  return (
    <div className="relative w-full mb-10 overflow-hidden rounded-[32px] border border-white/30 bg-white/10 backdrop-blur-md shadow-glass group">
      <div
        className="flex transition-transform duration-700 ease-in-out h-64 sm:h-80"
        style={{ transform: `translateX(-${currentIndex * 100}%)` }}
      >
        {items.map((item) => (
          <div key={item.id} className="w-full flex-shrink-0 flex items-center p-6 sm:p-10 gap-8">
            <div className="w-1/3 sm:w-1/2 h-full flex justify-center items-center bg-transparent">
              {item.image ? (
                <img
                  src={item.image}
                  alt={item.name}
                  className="max-h-full max-w-full object-contain drop-shadow-xl"
                />
              ) : (
                <span className="text-4xl font-bold text-black-blue/30">{item.name.charAt(0)}</span>
              )}
            </div>

            <div className="w-2/3 sm:w-1/2 flex flex-col justify-center">
              {item.orgName && (
                <Link to="#" className="text-sm font-semibold text-black-blue/60 hover:text-black-blue hover:underline mb-2">
                  {item.orgName}
                </Link>
              )}
              <h2 className="text-2xl sm:text-4xl font-bold text-black-blue mb-3">{item.name}</h2>
              {item.desc && (
                <p className="text-black-blue/70 text-sm sm:text-base line-clamp-3 mb-4">{item.desc}</p>
              )}
            </div>
          </div>
        ))}
      </div>

      <div className="absolute bottom-5 left-0 right-0 flex justify-center items-center gap-2.5">
        {items.map((_, idx) => (
          <button
            key={idx}
            onClick={() => setCurrentIndex(idx)}
            className={`transition-all duration-500 rounded-full ${currentIndex === idx ? 'w-8 h-2 bg-black-blue/60' : 'w-2 h-2 bg-black-blue/20 hover:bg-black-blue/40'
              }`}
          />
        ))}
      </div>
    </div>
  );
}